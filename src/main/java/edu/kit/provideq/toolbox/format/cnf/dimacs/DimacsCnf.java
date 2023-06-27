package edu.kit.provideq.toolbox.format.cnf.dimacs;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;
import edu.kit.provideq.toolbox.exception.ConversionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DimacsCnf {
  static final String LINE_SEPARATOR = System.lineSeparator();
  static final char SEPARATOR = ' ';
  static final char NEGATION_PREFIX = '-';
  static final char COMMENT_START = 'c';
  static final char PREAMBLE_START = 'p';
  static final char CLAUSE_END = '0';
  static final String CNF_IDENTIFIER = "cnf";

  private final List<Variable> variables;
  private final ArrayList<ArrayList<Variable>> orClauses;

  public DimacsCnf(Expression<String> expression) {
    this(new ExpressionToDimacsCnf().parse(expression));
  }

  public DimacsCnf(DimacsCnf cnf) {
    this.orClauses = new ArrayList<>(cnf.orClauses);
    this.variables = new ArrayList<>(cnf.variables);
  }

  public DimacsCnf(ArrayList<ArrayList<Variable>> orClauses) {
    this(orClauses, orClauses.stream()
        .flatMap(Collection::stream)
        .distinct()
        .toList());
  }

  public DimacsCnf(ArrayList<ArrayList<Variable>> orClauses, List<Variable> variables) {
    this.variables = List.copyOf(variables);
    this.orClauses = new ArrayList<>(orClauses);
  }

  /**
   * Create a dimacs cnf structure from a logical expression or string in format of dimacs cnf
   *
   * @param string logical expression or dimacs cnf string
   * @return dimacs cnf structure
   */
  public static DimacsCnf fromString(String string) throws ConversionException {
    var x =
        String.valueOf(DimacsCnf.PREAMBLE_START) + DimacsCnf.SEPARATOR + DimacsCnf.CNF_IDENTIFIER;

    return string.contains(x)
        ? fromDimacsCnfString(string)
        : fromLogicalExpressionString(string);
  }

  /**
   * Create a dimacs cnf structure from a string in format of dimacs cnf
   *
   * @param dimacsCnf dimacs cnf string
   * @return dimacs cnf structure
   */
  public static DimacsCnf fromDimacsCnfString(String dimacsCnf) throws ConversionException {
    return StringToDimacsCnf.parse(dimacsCnf);
  }

  /**
   * Create a dimacs cnf structure from a logical expression
   *
   * @param expression logical expression
   * @return dimacs cnf structure
   */
  public static DimacsCnf fromLogicalExpressionString(String expression) {
    // Streamline bool expr format
    expression = expression
        .replaceAll("\\b(?:not|NOT)\\b", "!")
        .replaceAll("\\b(?:and|AND)\\b", "&")
        .replaceAll("\\b(?:or|OR)\\b", "|");

    Expression<String> parsedExpression = ExprParser.parse(expression);
    Expression<String> cnfExpression = RuleSet.toCNF(parsedExpression);

    return new DimacsCnf(cnfExpression);
  }

  /**
   * Returns a list of variables.
   * The negation state of the variables doesn't have any meaning.
   *
   * @return list of variables
   */
  public Collection<Variable> getVariables() {
    return Collections.unmodifiableCollection(variables);
  }

  /**
   * Returns a list of or clauses
   *
   * @return list of or clauses where the inner lists is an or clauses of variables
   */
  public ArrayList<ArrayList<Variable>> getOrClauses() {
    return orClauses;
  }

  public DimacsCnf addOrClause(ArrayList<Variable> orClause) {
    var newOrClauses = new ArrayList<>(orClauses);
    newOrClauses.add(orClause);

    return new DimacsCnf(newOrClauses, Collections.unmodifiableList(variables));
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
