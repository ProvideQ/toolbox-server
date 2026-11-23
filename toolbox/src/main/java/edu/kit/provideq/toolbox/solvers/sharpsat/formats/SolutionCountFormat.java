package edu.kit.provideq.toolbox.solvers.sharpsat.formats;

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
 * The solution count format for #SAT problems.
 *
 * <p>The representation is a single line {@code count: <number>}.
 *
 * <p>Example instance:
 * <pre>
 * count: 4
 * </pre>
 */
public final class SolutionCountFormat implements Format<SolutionCount> {
  public static final SolutionCountFormat INSTANCE = new SolutionCountFormat();

  private static final String PREFIX = "count: ";

  private SolutionCountFormat() {
  }

  @Override
  public String getName() {
    return "solution-count";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("count");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<SolutionCount> getRepresentationClass() {
    return SolutionCount.class;
  }

  @Override
  public SolutionCount parse(Reader reader) throws ParseException {
    try {
      var text = FormatReader.readAll(reader).trim();
      if (!text.startsWith(PREFIX)) {
        throw new ParseException(
            "Solution count must start with '" + PREFIX + "', but was: " + text);
      }
      try {
        return new SolutionCount(Long.parseLong(text.substring(PREFIX.length()).trim()));
      } catch (NumberFormatException e) {
        throw new ParseException("Invalid solution count: " + text, e);
      }
    } catch (IOException e) {
      throw new ParseException("Could not read solution count", e);
    }
  }

  @Override
  public void serialize(SolutionCount value, Writer writer) throws SerializationException {
    try {
      writer.write(PREFIX + value.count() + System.lineSeparator());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize solution count", e);
    }
  }
}