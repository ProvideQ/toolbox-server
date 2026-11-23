package edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.voidmodel.formats;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Standardized line-based format for void-feature-model solutions.
 *
 * <p>The first line is {@code void: true} or {@code void: false}. If the model is not void, each
 * following non-empty line contains one entry of an example configuration as
 * {@code <feature name>: <true|false>}.
 *
 * <p>Example instance:
 * <pre>
 * feature1: false
 * feature2: true
 * feature3: true
 * feature4: false
 * </pre>
 */
public final class VoidFeatureSolutionFormat implements Format<VoidFeatureSolution> {
  public static final VoidFeatureSolutionFormat INSTANCE = new VoidFeatureSolutionFormat();

  private static final String VOID_PREFIX = "void: ";
  private static final String ENTRY_SEPARATOR = ": ";

  private VoidFeatureSolutionFormat() {
  }

  @Override
  public String getName() {
    return "void-feature-solution";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("void-model-solution");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("vfs");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<VoidFeatureSolution> getRepresentationClass() {
    return VoidFeatureSolution.class;
  }

  @Override
  public VoidFeatureSolution parse(Reader reader) throws ParseException {
    try {
      var lines = FormatReader.readAll(reader).lines()
          .map(String::trim)
          .filter(line -> !line.isEmpty())
          .toList();
      if (lines.isEmpty()) {
        throw new ParseException("Void-feature solution is empty");
      }

      var isVoid = parseVoidState(lines.getFirst());

      var exampleConfiguration = new LinkedHashMap<String, Boolean>();
      if (!isVoid) {
        for (var line : lines.subList(1, lines.size())) {
          var separatorIndex = line.lastIndexOf(ENTRY_SEPARATOR);
          if (separatorIndex <= 0) {
            throw new ParseException("Invalid configuration entry: " + line);
          }
          var name = line.substring(0, separatorIndex).trim();
          var state = parseState(line.substring(separatorIndex + ENTRY_SEPARATOR.length()));
          exampleConfiguration.put(name, state);
        }
      }

      return new VoidFeatureSolution(isVoid, () -> exampleConfiguration);
    } catch (IOException e) {
      throw new ParseException("Could not read void-feature solution", e);
    }
  }

  /**
   * Serializes a {@link VoidFeatureSolution} to the given writer.
   * The format looks like this:
   * <pre>
   *   void: true
   * </pre>
   *   or
   * <pre>
   *   void: false
   *   feature1: true
   *   feature2: false
   *   ...
   * </pre>
   *
   * @param value object representation to serialize
   * @param writer target of the serialized output
   * @throws SerializationException if there is an error during serialization
   */
  @Override
  public void serialize(VoidFeatureSolution value, Writer writer) throws SerializationException {
    var builder = new StringBuilder();
    builder.append(VOID_PREFIX).append(value.isVoid());
    if (!value.isVoid()) {
      value.exampleConfiguration().get()
          .entrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> builder.append(System.lineSeparator())
              .append(entry.getKey())
              .append(ENTRY_SEPARATOR)
              .append(entry.getValue()));
    }
    builder.append(System.lineSeparator());

    try {
      writer.write(builder.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize void-feature solution", e);
    }
  }

  private static boolean parseVoidState(String line) throws ParseException {
    if (!line.startsWith(VOID_PREFIX)) {
      throw new ParseException(
          "Void-feature solution must start with '" + VOID_PREFIX + "true' or '"
              + VOID_PREFIX + "false', but was: " + line);
    }
    return parseState(line.substring(VOID_PREFIX.length()));
  }

  private static boolean parseState(String state) throws ParseException {
    return switch (state.toLowerCase(Locale.ROOT)) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new ParseException("Invalid boolean state: " + state);
    };
  }
}