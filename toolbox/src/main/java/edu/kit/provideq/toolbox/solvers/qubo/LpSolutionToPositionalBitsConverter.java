package edu.kit.provideq.toolbox.solvers.qubo;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ContextConverter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.lp.LPSolutionFormat;
import java.util.LinkedHashSet;

/**
 * Converts an {@link LPSolutionFormat lp-solution} into a
 * {@link PositionalBitsSolutionFormat positional-bits solution}.
 *
 * <p>The conversion requires the problem instance as context: the bits are ordered by
 * the variables declared in the LP file's {@code Binary} section, so the LP problem text
 * is passed to {@link #convert(String, String)}.
 *
 * <p>Example conversion for an LP file declaring {@code Binary x1 x2 x3 x4}:
 * <pre>
 * input:  x1 0        output: 0 1 0 1
 *         x2 1
 *         x3 0
 *         x4 1
 * </pre>
 */
public final class LpSolutionToPositionalBitsConverter
    implements ContextConverter<String, String, String> {
  /**
   * Singleton instance of the converter.
   */
  public static final LpSolutionToPositionalBitsConverter INSTANCE =
      new LpSolutionToPositionalBitsConverter();

  private LpSolutionToPositionalBitsConverter() {
  }

  @Override
  public Format<String> getSource() {
    return LPSolutionFormat.INSTANCE;
  }

  @Override
  public Format<String> getTarget() {
    return PositionalBitsSolutionFormat.INSTANCE;
  }

  @Override
  public Class<String> getContextClass() {
    return String.class;
  }

  @Override
  public String convert(String lpSolution, String lpProblemText) throws FormatException {
    var variables = readBinaryVariables(lpProblemText);
    var assignments = LPSolutionFormat.parseAssignments(lpSolution);

    var bits = new StringBuilder();
    for (var variable : variables) {
      var value = assignments.getOrDefault(variable, 0.0);
      bits.append(value != 0 ? "1" : "0").append(' ');
    }
    return bits.toString().trim();
  }

  /**
   * Reads the variables declared in the LP file's {@code Binary} section, in declaration
   * order.
   */
  static LinkedHashSet<String> readBinaryVariables(String lpText)
      throws FormatException {
    var variables = new LinkedHashSet<String>();
    var inBinarySection = false;
    for (var line : lpText.split("\r?\n")) {
      var trimmed = line.trim();
      if (trimmed.equalsIgnoreCase("binary")) {
        inBinarySection = true;
        continue;
      }
      if (inBinarySection) {
        if (trimmed.equalsIgnoreCase("end") || trimmed.isEmpty()) {
          break;
        }
        for (var variable : trimmed.split("\\s+")) {
          if (!variables.add(variable)) {
            throw new FormatException("Duplicate variable in Binary section: " + variable);
          }
        }
      }
    }
    if (variables.isEmpty()) {
      throw new ParseException("LP file declares no variables in a Binary section");
    }
    return variables;
  }
}
