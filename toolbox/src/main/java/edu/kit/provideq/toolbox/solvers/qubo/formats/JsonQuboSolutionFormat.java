package edu.kit.provideq.toolbox.solvers.qubo.formats;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import edu.kit.provideq.toolbox.solvers.qubo.formats.converters.JsonQuboToLpSolutionConverter;
import edu.kit.provideq.toolbox.solvers.qubo.formats.converters.LpSolutionToJsonQuboConverter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The json QUBO solution format emitted by the GAMS CPLEX QUBO solver.
 *
 * <p>The representation is a JSON object mapping variable names to their (binary) value,
 * containing <em>only the variables assigned to 1</em>; all omitted variables are 0.
 *
 * <p>Example instance for a QUBO with the variables {@code x1 x2 x3 x4}, where {@code x1}
 * and {@code x4} are set to 1 and {@code x2}, {@code x3} are 0:
 * <pre>
 * {
 *     "x1": 1.0,
 *     "x4": 1.0
 * }
 * </pre>
 *
 * <p>Use {@link LpSolutionToJsonQuboConverter} and {@link JsonQuboToLpSolutionConverter}
 * to convert between this format and the {@link LPSolutionFormat lp-solution} format.
 */
public final class JsonQuboSolutionFormat implements Format<String> {
  private static final ObjectMapper JSON = new ObjectMapper();

  /**
   * Singleton instance of the json QUBO solution format.
   */
  public static final JsonQuboSolutionFormat INSTANCE = new JsonQuboSolutionFormat();

  private JsonQuboSolutionFormat() {
  }

  @Override
  public String getName() {
    return "json-qubo-solution";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("json-sol");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<String> getRepresentationClass() {
    return String.class;
  }

  @Override
  public String parse(Reader reader) throws ParseException {
    String text;
    try {
      text = FormatReader.readAll(reader);
    } catch (IOException e) {
      throw new ParseException("Could not read json QUBO solution", e);
    }
    parseAssignments(text);
    return text;
  }

  /**
   * Parses the assignments of a json QUBO solution: a JSON object mapping variable names
   * to their binary value, containing only the variables assigned to 1.
   *
   * @param text json QUBO solution JSON text
   * @return the assignments in order of appearance
   * @throws ParseException if the text is not a JSON object of numeric variable assignments
   */
  public static Map<String, Double> parseAssignments(String text) throws ParseException {
    Map<String, Object> raw;
    try {
      raw = JSON.readValue(
          text, new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {
          });
    } catch (IOException e) {
      throw new ParseException(
          "Json QUBO solution must be a JSON object of variable assignments: "
              + e.getMessage());
    }
    var assignments = new LinkedHashMap<String, Double>();
    for (var entry : raw.entrySet()) {
      if (!(entry.getValue() instanceof Number number)) {
        throw new ParseException(("Json QUBO solution assignment for variable \"%s\" must be "
            + "a number, but was: %s").formatted(entry.getKey(), entry.getValue()));
      }
      assignments.put(entry.getKey(), number.doubleValue());
    }
    return assignments;
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize json QUBO solution", e);
    }
  }
}
