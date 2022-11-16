package edu.kit.provideq.toolbox.sat.convert;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Variable;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

import java.util.HashMap;

public class BoolExprToDimacsCNF {
    static String lineSeparator = System.getProperty("line.separator");
    static String negationPrefix = "-";

    public static String Convert(String boolExpr) {
        Expression<String> parsedExpression = ExprParser.parse(boolExpr);
        Expression<String> cnfExpression = RuleSet.toCNF(parsedExpression);

        return GetDimacsCNF(cnfExpression);
    }

    public static String GetDimacsCNF(Expression<String> cnfExpression) {
        var builder = new StringBuilder();

        var varNameMap = new HashMap<String, Integer>();
        var nextVarNumber = 1;
        var varCount = 0;
        var exprCount = 0;

        //Add clauses
        for (Expression<String> orExpression : cnfExpression.getChildren()) {
            for (Expression<String> atomicExpression : orExpression.getChildren()) {
                var isNegated = false;

                //Handle negated variables
                String varName;
                if (atomicExpression instanceof Variable<String>) {
                    varName = ((Variable<String>) atomicExpression).getValue();
                } else {
                    isNegated = true;
                    varName = ((Variable<String>) atomicExpression.getChildren().get(0)).getValue();
                }

                //Cache variable number
                Integer varNumber = varNameMap.get(varName);
                if (varNumber == null) {
                    varNumber = nextVarNumber;
                    nextVarNumber++;
                    varCount++;

                    varNameMap.put(varName, varNumber);
                }

                if (isNegated) builder.append(negationPrefix);
                builder.append(varNumber);
                builder.append(" ");
            }

            exprCount++;
            builder.append("0");
            builder.append(lineSeparator);
        }

        //Add preamble problem line
        builder.insert(0, "p cnf %d %d%s".formatted(varCount, exprCount, lineSeparator));

        return builder.toString();
    }
}
