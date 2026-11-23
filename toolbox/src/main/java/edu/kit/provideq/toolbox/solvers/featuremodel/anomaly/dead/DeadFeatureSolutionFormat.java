package edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

/**
 * Standard newline-delimited format for dead feature names.
 *
 * <p>Each non-empty line contains exactly one feature name. An empty document represents an
 * empty dead-feature list.
 *
 * <p>Example instance:
 * <pre>
 * feature1
 * feature2
 * </pre>
 */
public final class DeadFeatureSolutionFormat implements Format<DeadFeatureSolution> {
  public static final DeadFeatureSolutionFormat INSTANCE = new DeadFeatureSolutionFormat();

  private DeadFeatureSolutionFormat() {
  }

  @Override
  public String getName() {
    return "dead-feature-list";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("dead-features");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("dfl");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<DeadFeatureSolution> getRepresentationClass() {
    return DeadFeatureSolution.class;
  }

  @Override
  public DeadFeatureSolution parse(Reader reader) throws ParseException {
    try {
      var names = FormatReader.readAll(reader).lines()
          .map(String::trim)
          .filter(line -> !line.isEmpty())
          .toList();
      return new DeadFeatureSolution(names);
    } catch (IOException e) {
      throw new ParseException("Could not read dead-feature list", e);
    }
  }

  /**
   * Serializes a dead-feature solution to a newline-delimited list of feature names.
   * The format looks like this:
   * <pre>
   *   feature1
   *   feature2
   *   feature3
   * </pre>
   *
   * @param value object representation to serialize
   * @param writer target of the serialized output
   * @throws SerializationException if there is an error during serialization
   */
  @Override
  public void serialize(DeadFeatureSolution value, Writer writer) throws SerializationException {
    try {
      writer.write(String.join(System.lineSeparator(), value.featureNames()));
      if (!value.featureNames().isEmpty()) {
        writer.write(System.lineSeparator());
      }
    } catch (IOException e) {
      throw new SerializationException("Could not serialize dead-feature list", e);
    }
  }
}