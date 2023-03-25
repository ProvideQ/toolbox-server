package edu.kit.provideq.toolbox.convert;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Variable;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public final class BoolExprToDimacsCNF {
    private final static String lineSeparator = System.getProperty("line.separator");
    private final static String negationPrefix = "-";

    public static String convert(String boolExpr) {
        if (boolExpr.contains("p cnf")) return boolExpr;

        // Streamline bool expr format
        boolExpr = boolExpr
                .replaceAll("\\b(?:not|NOT)\\b", "!")
                .replaceAll("\\b(?:and|AND)\\b", "&")
                .replaceAll("\\b(?:or|OR)\\b", "|");

        Expression<String> parsedExpression = ExprParser.parse(boolExpr);
        Expression<String> cnfExpression = RuleSet.toCNF(parsedExpression);

        return getDimacsCNF(cnfExpression);
    }

    public static String getDimacsCNF(Expression<String> cnfExpression) {
        var builder = new StringBuilder();

        var varNameMap = new HashMap<String, Integer>();
        var ref = new Object() {
            int nextVarNumber = 1;
            int varCount = 0;
        };
        var exprCount = 0;

        Function<Expression<String>, String> GetAtomicCNFExpression = expression -> {
            var isNegated = false;

            //Handle negated variables
            String varName;
            if (expression instanceof Variable<String>) {
                varName = ((Variable<String>) expression).getValue();
            } else {
                isNegated = true;
                varName = ((Variable<String>) expression.getChildren().get(0)).getValue();
            }

            //Cache variable number
            Integer varNumber = varNameMap.get(varName);
            if (varNumber == null) {
                varNumber = ref.nextVarNumber;
                ref.nextVarNumber++;
                ref.varCount++;

                varNameMap.put(varName, varNumber);
            }

            var x = new StringBuilder();
            if (isNegated) x.append(negationPrefix);
            x.append(varNumber);
            return x.toString();
        };

        //Add clauses
        for (Expression<String> orExpression : cnfExpression.getChildren()) {
            List<Expression<String>> children = orExpression.getChildren();
            if (children.size() == 0 || orExpression instanceof Not<String>) {
                builder.append(GetAtomicCNFExpression.apply(orExpression));
                builder.append(" ");
            } else {
                for (Expression<String> atomicExpression : children) {
                    builder.append(GetAtomicCNFExpression.apply(atomicExpression));
                    builder.append(" ");
                }
            }

            exprCount++;
            builder.append("0");
            builder.append(lineSeparator);
        }

        //Add preamble problem line
        builder.insert(0, "p cnf %d %d%s".formatted(ref.varCount, exprCount, lineSeparator));

        return builder.toString();
    }
}
