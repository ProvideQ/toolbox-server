package edu.kit.provideq.toolbox.solvers.qubo.formats.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.Converter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.lp.formats.LPSolutionFormat;
import edu.kit.provideq.toolbox.solvers.qubo.formats.JsonQuboSolutionFormat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts an {@link LPSolutionFormat lp-solution} into a
 * {@link JsonQuboSolutionFormat json QUBO solution}: variables assigned to 1 are kept,
 * variables assigned to 0 are omitted.
 *
 * <p>Example conversion:
 * <pre>
 * input:  x1 0          output: {
 *         x2 1                    "x2": 1.0,
 *         x3 0                    "x4": 1.0
 *         x4 1                  }
 * </pre>
 */
public class LpSolutionToJsonQuboConverter implements Converter<String, String> {
  private static final ObjectMapper JSON = new ObjectMapper();

  /**
   * Singleton instance of the converter.
   */
  public static final LpSolutionToJsonQuboConverter INSTANCE =
      new LpSolutionToJsonQuboConverter();

  @Override
  public Format<String> getSource() {
    return LPSolutionFormat.INSTANCE;
  }

  @Override
  public Format<String> getTarget() {
    return JsonQuboSolutionFormat.INSTANCE;
  }

  @Override
  public String convert(String lpSolution) throws FormatException {
    var assignments = LPSolutionFormat.parseAssignments(lpSolution);

    Map<String, Double> active = new LinkedHashMap<>();
    for (var assignment : assignments.entrySet()) {
      var value = assignment.getValue();
      if (value != 0 && value != 1) {
        throw new FormatException(("QUBO solution variable \"%s\" must be binary (0 or 1), "
            + "but was: %s").formatted(assignment.getKey(), value));
      }
      if (value == 1) {
        active.put(assignment.getKey(), 1.0);
      }
    }

    try {
      return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(active);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new FormatException("Could not serialize json QUBO solution: " + e.getMessage());
    }
  }
}
