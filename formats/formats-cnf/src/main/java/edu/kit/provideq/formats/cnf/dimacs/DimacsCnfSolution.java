package edu.kit.provideq.formats.cnf.dimacs;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Object representation of a solution to a DIMACS CNF formula.
 */
public class DimacsCnfSolution {
  public static final char SOLUTION_START = 's';
  public static final char VARIABLE_DECLARATION = 'v';

  private final DimacsCnf dimacsCnf;

  /**
   * Unmodifiable mapping from a variable to its boolean state.
   */
  private final Map<Variable, Boolean> variableMap;

  public DimacsCnfSolution(DimacsCnf dimacsCnf, Map<Variable, Boolean> variableMap) {
    this.dimacsCnf = dimacsCnf;
    this.variableMap = Map.copyOf(variableMap);
  }

  public static DimacsCnfSolution fromString(DimacsCnf dimacsCnf, String solutionString) {
    Map<Integer, Boolean> variableMap = StringToDimacsCnfSolution.parse(solutionString);

    Map<Variable, Boolean> namedVariableMap = dimacsCnf
        .getVariables()
        .stream()
        .filter(v -> variableMap.containsKey(v.number()))
        .collect(Collectors.toMap(
            Function.identity(),
            variable -> variableMap.get(variable.number())
        ));

    return new DimacsCnfSolution(dimacsCnf, namedVariableMap);
  }

  /**
   * Returns an unmodifiable mapping from a variable to its boolean state.
   *
   * @return unmodifiable map from a variable to the boolean state of the variable
   */
  public Map<Variable, Boolean> getVariableMapping() {
    return variableMap;
  }

  public boolean isVoid() {
    return variableMap.isEmpty();
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    // Add variable names as comment
    DimacsCnf.addVariableComments(builder, variableMap.keySet());

    // Add preamble
    builder.append(SOLUTION_START)
        .append(DimacsCnf.SEPARATOR)
        .append(DimacsCnf.CNF_IDENTIFIER)
        .append(DimacsCnf.SEPARATOR)
        .append("1")
        .append(DimacsCnf.SEPARATOR)
        .append(variableMap.size())
        .append(DimacsCnf.SEPARATOR)
        .append(dimacsCnf.getOrClauses().size())
        .append(System.lineSeparator());

    // Add variable declarations
    for (Map.Entry<Variable, Boolean> variableBooleanEntry : variableMap.entrySet()) {

      var number = variableBooleanEntry.getKey().number();
      var variable = Boolean.TRUE.equals(variableBooleanEntry.getValue())
          ? number
          : Variable.NEGATION_PREFIX + String.valueOf(number);

      builder.append(VARIABLE_DECLARATION)
          .append(DimacsCnf.SEPARATOR)
          .append(variable)
          .append(System.lineSeparator());
    }

    return builder.toString();
  }

  public String toHumanReadableString() {
    var builder = new StringBuilder();

    for (Map.Entry<Variable, Boolean> variableBooleanEntry : variableMap.entrySet()) {
      builder.append(variableBooleanEntry.getKey().name())
          .append(": ")
          .append(variableBooleanEntry.getValue())
          .append(System.lineSeparator());
    }

    return builder.toString();
  }
}
