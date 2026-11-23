package edu.kit.provideq.toolbox.solvers.qubo.formats;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import edu.kit.provideq.formats.core.families.ContextBoundFormat;
import edu.kit.provideq.toolbox.solvers.qubo.formats.converters.LpSolutionToPositionalBitsConverter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

/**
 * The positional-bits QUBO solution format consumed by the hybrid VRP pipeline binary.
 *
 * <p>The representation is a single line of whitespace-separated bits, one per variable,
 * ordered by the variables declared in the LP file's {@code Binary} section. Variables
 * without an assignment default to 0.
 *
 * <p>Example instance for a QUBO with the variables {@code x1 x2 x3 x4} in its
 * {@code Binary} section, where {@code x2} and {@code x4} are set to 1:
 * <pre>
 * 0 1 0 1
 * </pre>
 *
 * <p>The format is a context-bound member of the {@code QUBO-Solution} family: converting
 * between it and the other family members requires the LP problem text as context (the bits
 * are ordered by the {@code Binary} section), so the conversion is performed by the
 * context-bound {@link LpSolutionToPositionalBitsConverter} instead of a registry converter.
 */
public final class PositionalBitsSolutionFormat implements Format<String>, ContextBoundFormat {
  /**
   * Singleton instance of the positional bits solution format.
   */
  public static final PositionalBitsSolutionFormat INSTANCE =
      new PositionalBitsSolutionFormat();

  private PositionalBitsSolutionFormat() {
  }

  @Override
  public String getName() {
    return "qubo-positional-solution";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("positional-sol");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<?> getRepresentationClass() {
    return String.class;
  }

  @Override
  public String parse(Reader reader) throws ParseException {
    try {
      var text = FormatReader.readAll(reader).trim();
      if (text.isEmpty()) {
        throw new ParseException("Positional bits solution must contain at least one bit");
      }
      for (var bit : text.split("\\s+")) {
        if (!bit.equals("0") && !bit.equals("1")) {
          throw new ParseException(
              "Positional bits solution must only contain 0 and 1, but was: %s".formatted(bit));
        }
      }
      return text;
    } catch (IOException e) {
      throw new ParseException("Could not read positional bits solution", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize positional bits solution", e);
    }
  }
}
