package edu.kit.provideq.formats.tsplib.formats;

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
 * TSPLIB TOUR solution format emitted by TSP solvers.
 *
 * <p>The representation is a TSPLIB tour document: an optional {@code TOUR_SECTION} header
 * followed by one stop index per line, terminated by {@code -1} and {@code EOF}.
 * The format declares this structure so the data can be addressed deterministically;
 * attribute readers can be added as needed.
 *
 * <p>Example instance:
 * <pre>
 * TOUR_SECTION
 * 1
 * 2
 * 3
 * -1
 * EOF
 * </pre>
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record TSPLIBSolutionFormat() implements Format<String> {
  /**
   * Singleton instance of the TSPLIB TOUR solution format.
   */
  public static final TSPLIBSolutionFormat INSTANCE = new TSPLIBSolutionFormat();

  @Override
  public String getName() {
    return "tsplib-tour";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("tour");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("tour");
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
    try {
      return FormatReader.readAll(reader);
    } catch (IOException e) {
      throw new ParseException("Could not read TSPLIB tour input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize TSPLIB tour", e);
    }
  }
}