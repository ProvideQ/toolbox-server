package edu.kit.provideq.toolbox.format.cnf.dimacs;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        this.orClauses = new ArrayList<>(cnf.orClauses);
        this.variables = new ArrayList<>(cnf.variables);
    }

    public DimacsCNF(ArrayList<ArrayList<Variable>> orClauses) {
        this(orClauses, orClauses.stream()
                .flatMap(Collection::stream)
                .distinct()
                .toList());
    }

    public DimacsCNF(ArrayList<ArrayList<Variable>> orClauses, List<Variable> variables) {
        this.variables = variables;
        this.orClauses = orClauses;
    }

    /**
     * Create a dimacs cnf structure from a logical expression or string in format of dimacs cnf
     * @param string logical expression or dimacs cnf string
     * @return dimacs cnf structure
     */
    public static DimacsCNF fromString(String string) {
        var x = String.valueOf(DimacsCNF.PREAMBLE_START) + DimacsCNF.SEPARATOR + DimacsCNF.CNF_IDENTIFIER;

        return string.contains(x)
                ? fromDimacsCNFString(string)
                : fromLogicalExpressionString(string);
    }

    /**
     * Create a dimacs cnf structure from a string in format of dimacs cnf
     * @param dimacsCNF dimacs cnf string
     * @return dimacs cnf structure
     */
    public static DimacsCNF fromDimacsCNFString(String dimacsCNF) {
        return StringToDimacsCNF.parse(dimacsCNF);
    }

    /**
     * Create a dimacs cnf structure from a logical expression
     * @param expression logical expression
     * @return dimacs cnf structure
     */
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

    public Collection<Variable> getVariables() {
        return Collections.unmodifiableCollection(variables);
    }

    /**
     * Returns a list of or clauses
     * @return list of or clauses where the inner lists is an or clauses of variables
     */
    public ArrayList<ArrayList<Variable>> getOrClauses() {
        return orClauses;
    }

    public DimacsCNF addOrClause(ArrayList<Variable> orClause) {
        var newOrClauses = new ArrayList<>(orClauses);
        newOrClauses.add(orClause);

        return new DimacsCNF(newOrClauses, Collections.unmodifiableList(variables));
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        // Add variable names as comment
        addVariableComments(builder, variables);

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

    static void addVariableComments(StringBuilder builder, Collection<Variable> variables) {
        // Add variable names comments like this
        // c 42 apple
        for (Variable variable : variables) {
            builder.append(COMMENT_START)
                    .append(SEPARATOR)
                    .append(variable.number())
                    .append(SEPARATOR)
                    .append(variable.name())
                    .append(LINE_SEPARATOR);
        }
    }
}
