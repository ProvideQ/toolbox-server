package edu.kit.provideq.toolbox.format.cnf.dimacs;

import static edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf.CNF_IDENTIFIER;
import static edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf.LINE_SEPARATOR;
import static edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf.NEGATION_PREFIX;
import static edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf.SEPARATOR;
import static edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf.addVariableComments;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
   * Return an unmodifiable mapping from a variable to its boolean state.
   *
   * @return unmodifiable map from a variable to the boolean state of the variable
   */
  public Map<Variable, Boolean> getVariableMapping() {
    return variableMap;
  }

  public boolean isVoid() {
    return variableMap.size() == 0;
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    // Add variable names as comment
    addVariableComments(builder, variableMap.keySet());

    // Add preamble
    builder.append(SOLUTION_START)
        .append(SEPARATOR)
        .append(CNF_IDENTIFIER)
        .append(SEPARATOR)
        .append("1")
        .append(SEPARATOR)
        .append(variableMap.size())
        .append(SEPARATOR)
        .append(dimacsCnf.getOrClauses().size())
        .append(LINE_SEPARATOR);

    // Add variable declarations
    for (Map.Entry<Variable, Boolean> variableBooleanEntry : variableMap.entrySet()) {

      var number = variableBooleanEntry.getKey().number();
      var variable = variableBooleanEntry.getValue()
          ? number
          : String.valueOf(NEGATION_PREFIX) + number;

      builder.append(VARIABLE_DECLARATION)
          .append(SEPARATOR)
          .append(variable)
          .append(LINE_SEPARATOR);
    }

    return builder.toString();
  }

  public String toHumanReadableString() {
    var builder = new StringBuilder();

    for (Map.Entry<Variable, Boolean> variableBooleanEntry : variableMap.entrySet()) {
      builder.append(variableBooleanEntry.getKey().name())
          .append(": ")
          .append(variableBooleanEntry.getValue())
          .append(LINE_SEPARATOR);
    }

    return builder.toString();
  }
}
