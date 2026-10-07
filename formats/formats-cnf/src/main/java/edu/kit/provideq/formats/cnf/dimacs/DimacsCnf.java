package edu.kit.provideq.formats.cnf.dimacs;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;
import edu.kit.provideq.formats.core.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Object representation of a DIMACS CNF formula.
 */
public class DimacsCnf {
  public static final char COMMENT_START = 'c';
  public static final char PREAMBLE_START = 'p';
  public static final char CLAUSE_END = '0';
  public static final String CNF_IDENTIFIER = "cnf";
  public static final char SEPARATOR = ' ';

  private final List<Variable> variables;
  private final List<List<Variable>> orClauses;

  public DimacsCnf(Expression<String> expression) throws ParseException {
    this(new ExpressionToDimacsCnf().parse(expression));
  }

  public DimacsCnf(DimacsCnf cnf) {
    this(cnf.orClauses, cnf.variables);
  }

  public DimacsCnf(List<List<Variable>> orClauses) {
    this(orClauses, orClauses.stream()
        .flatMap(Collection::stream)
        .distinct()
        .toList());
  }

  public DimacsCnf(List<List<Variable>> orClauses, List<Variable> variables) {
    this.variables = Collections.unmodifiableList(variables);

    var clauseList = new ArrayList<List<Variable>>(orClauses.size());
    for (var clause : orClauses) {
      clauseList.add(List.copyOf(clause));
    }
    this.orClauses = Collections.unmodifiableList(clauseList);
  }

  /**
   * Creates a DIMACS CNF structure from a logical expression or a string in DIMACS CNF format.
   *
   * @param string logical expression or DIMACS CNF string
   * @return DIMACS CNF structure
   */
  public static DimacsCnf fromString(String string) throws ParseException {
    var preamble = String.valueOf(PREAMBLE_START) + SEPARATOR + CNF_IDENTIFIER;

    return string.contains(preamble)
        ? fromDimacsCnfString(string)
        : fromLogicalExpressionString(string);
  }

  /**
   * Creates a DIMACS CNF structure from a string in DIMACS CNF format.
   *
   * @param dimacsCnf DIMACS CNF string
   * @return DIMACS CNF structure
   */
  public static DimacsCnf fromDimacsCnfString(String dimacsCnf) throws ParseException {
    return StringToDimacsCnf.parse(dimacsCnf);
  }

  /**
   * Creates a DIMACS CNF structure from a logical expression.
   *
   * @param expression logical expression
   * @return DIMACS CNF structure
   */
  public static DimacsCnf fromLogicalExpressionString(String expression) throws ParseException {
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
   * Returns the variables of this formula.
   * The negation state of the variables doesn't have any meaning.
   *
   * @return list of variables
   */
  public Collection<Variable> getVariables() {
    return variables;
  }

  /**
   * Returns the or-clauses of this formula.
   *
   * @return list of or-clauses where the inner list is an or-clause of variables
   */
  public List<List<Variable>> getOrClauses() {
    return orClauses;
  }

  public DimacsCnf addOrClause(List<Variable> orClause) {
    var newOrClauses = new ArrayList<>(orClauses);
    newOrClauses.add(orClause);

    return new DimacsCnf(newOrClauses, variables);
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
        .append(System.lineSeparator());

    // Add clauses
    // Example: 1 2 0
    for (List<Variable> clause : orClauses) {
      for (Variable variable : clause) {
        builder.append(variable.toString());
        builder.append(SEPARATOR);
      }

      builder.append(CLAUSE_END)
          .append(System.lineSeparator());
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
          .append(System.lineSeparator());
    }
  }
}
