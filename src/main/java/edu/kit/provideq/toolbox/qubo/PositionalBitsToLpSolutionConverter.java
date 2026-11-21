package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ContextConverter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.toolbox.formats.LPSolutionFormat;

/**
 * Converts a {@link PositionalBitsSolutionFormat positional-bits solution} into an
 * {@link LPSolutionFormat lp-solution}.
 *
 * <p>The conversion requires the problem instance as context: the bits are ordered by
 * the variables declared in the LP file's {@code Binary} section, so the LP problem text
 * is passed to {@link #convert(String, String)}. Only the variables assigned to 1 are
 * emitted as assignment lines; omitted variables are 0.
 *
 * <p>Example conversion for an LP file declaring {@code Binary x1 x2 x3 x4}:
 * <pre>
 * input:  0 1 0 1      output:  x2 1
 *                               x4 1
 * </pre>
 */
public final class PositionalBitsToLpSolutionConverter
    implements ContextConverter<String, String, String> {
  /**
   * Singleton instance of the converter.
   */
  public static final PositionalBitsToLpSolutionConverter INSTANCE =
      new PositionalBitsToLpSolutionConverter();

  private PositionalBitsToLpSolutionConverter() {
  }

  @Override
  public Format<String> getSource() {
    return PositionalBitsSolutionFormat.INSTANCE;
  }

  @Override
  public Format<String> getTarget() {
    return LPSolutionFormat.INSTANCE;
  }

  @Override
  public Class<String> getContextClass() {
    return String.class;
  }

  @Override
  public String convert(String positionalBits, String lpProblemText) throws FormatException {
    var variables = LpSolutionToPositionalBitsConverter.readBinaryVariables(lpProblemText);
    var bits = positionalBits.trim().split("\\s+");

    if (bits.length != variables.size()) {
      throw new FormatException(
          "Positional bits solution has %d bits, but the LP file declares %d variables"
              .formatted(bits.length, variables.size()));
    }

    var lpSolution = new StringBuilder();
    var index = 0;
    for (var variable : variables) {
      var bit = bits[index++];
      if (!bit.equals("0") && !bit.equals("1")) {
        throw new FormatException(
            "Positional bits solution must only contain 0 and 1, but was: %s".formatted(bit));
      }
      if (bit.equals("1")) {
        lpSolution.append(variable).append(" 1").append('\n');
      }
    }
    return lpSolution.toString();
  }
}
