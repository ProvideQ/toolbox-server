package edu.kit.provideq.toolbox.format.cnf.dimacs;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DimacsCNF {
    final static String LINE_SEPARATOR = System.getProperty("line.separator");
    final static char SEPARATOR = ' ';
    final static char NEGATION_PREFIX = '-';
    final static char COMMENT_START = 'c';
    final static char PREAMBLE_START = 'p';
    final static char CLAUSE_END = '0';
    final static String CNF_IDENTIFIER = "cnf";

    private final List<Variable> variables;
    private final ArrayList<ArrayList<Variable>> orClauses;

    public DimacsCNF(Expression<String> expression) {
        this(new ExpressionToDimacsCNF().parse(expression));
    }

    public DimacsCNF(DimacsCNF cnf) {
        this.orClauses = cnf.orClauses;
        this.variables = cnf.variables;
    }

    public DimacsCNF(ArrayList<ArrayList<Variable>> orClauses) {
        this(orClauses, orClauses.stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList()));
    }

    public DimacsCNF(ArrayList<ArrayList<Variable>> orClauses, List<Variable> variables) {
        this.variables = variables;
        this.orClauses = orClauses;
    }

    public static DimacsCNF fromString(String string) {
        var x = String.valueOf(DimacsCNF.PREAMBLE_START) + DimacsCNF.SEPARATOR + DimacsCNF.CNF_IDENTIFIER;

        return string.contains(x)
                ? fromDimacsCNFString(string)
                : fromLogicalExpressionString(string);
    }

    public static DimacsCNF fromDimacsCNFString(String dimacsCNF) {
        StringToDimacsCNF dimacsCNFReader = new StringToDimacsCNF();
        return dimacsCNFReader.parse(dimacsCNF);
    }

    public static DimacsCNF fromLogicalExpressionString(String expression) {
        // Streamline bool expr format
        expression = expression
                .replaceAll("\\b(?:not|NOT)\\b", "!")
                .replaceAll("\\b(?:and|AND)\\b", "&")
                .replaceAll("\\b(?:or|OR)\\b", "|");

        Expression<String> parsedExpression = ExprParser.parse(expression);
        Expression<String> cnfExpression = RuleSet.toCNF(parsedExpression);

        return new DimacsCNF(cnfExpression);
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        // Add variable names as comment
        for (Variable variable : variables) {
            builder.append(COMMENT_START)
                    .append(SEPARATOR)
                    .append(variable.name)
                    .append(SEPARATOR)
                    .append(variable.number)
                    .append(LINE_SEPARATOR);
        }

        // Add preamble problem line
        // Example for 3 clauses with 4 variables
        // p cnf 4 3
        builder.append(PREAMBLE_START)
                .append(SEPARATOR)
                .append(CNF_IDENTIFIER)
                .append(SEPARATOR)
                .append(variables.size())
                .append(SEPARATOR)
                .append(orClauses.size())
                .append(LINE_SEPARATOR);

        // Add clauses
        // Example: 1 2 0
        for (List<Variable> clause : orClauses) {
            for (Variable variable : clause) {
                builder.append(variable.toString());
                builder.append(SEPARATOR);
            }

            builder.append(CLAUSE_END)
                    .append(LINE_SEPARATOR);
        }

        return builder.toString();
    }
}
