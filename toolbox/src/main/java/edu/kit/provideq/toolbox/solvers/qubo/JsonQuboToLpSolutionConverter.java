package edu.kit.provideq.toolbox.solvers.qubo;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.Converter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.lp.LPSolutionFormat;

/**
 * Converts a {@link JsonQuboSolutionFormat json QUBO solution} into an
 * {@link LPSolutionFormat lp-solution}: every variable of the json assignment becomes a
 * {@code <variable> <value>} line; variables omitted by the json representation are 0 and
 * are not emitted.
 *
 * <p>Example conversion:
 * <pre>
 * input:  {             output:  x2 1
 *           "x2": 1.0,           x4 1
 *           "x4": 1.0
 *         }
 * </pre>
 */
public class JsonQuboToLpSolutionConverter implements Converter<String, String> {
  /**
   * Singleton instance of the converter.
   */
  public static final JsonQuboToLpSolutionConverter INSTANCE =
      new JsonQuboToLpSolutionConverter();

  @Override
  public Format<String> getSource() {
    return JsonQuboSolutionFormat.INSTANCE;
  }

  @Override
  public Format<String> getTarget() {
    return LPSolutionFormat.INSTANCE;
  }

  @Override
  public String convert(String jsonSolution) throws FormatException {
    var assignments = JsonQuboSolutionFormat.parseAssignments(jsonSolution);

    var lpSolution = new StringBuilder();
    for (var assignment : assignments.entrySet()) {
      var value = assignment.getValue();
      if (value != 0 && value != 1) {
        throw new FormatException(("QUBO solution variable \"%s\" must be binary (0 or 1), "
            + "but was: %s").formatted(assignment.getKey(), value));
      }
      if (value == 1) {
        lpSolution.append(assignment.getKey()).append(" 1").append('\n');
      }
    }
    return lpSolution.toString();
  }
}
