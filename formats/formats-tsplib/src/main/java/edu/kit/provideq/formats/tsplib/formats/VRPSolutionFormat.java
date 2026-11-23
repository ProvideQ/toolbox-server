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
 * Vehicle-tour solution format emitted by VRP solvers.
 *
 * <p>The representation is a tour document: one stop index per line, with depot returns
 * and route separators as emitted by the solver backends. The format declares this
 * structure so the data can be addressed deterministically; attribute readers can be
 * added as needed.
 *
 * <p>Example instance (as emitted by the LKH-3 backend for a 2-route solution over
 * 5 customers plus depot 1):
 * <pre>
 * NAME: small sample solution
 * TYPE: TOUR
 * TOUR_SECTION:
 * 1 2 -1
 * 1 4 5 3 -1
 * -1
 * EOF
 * </pre>
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record VRPSolutionFormat() implements Format<String> {
  /**
   * Singleton instance of the VRP tour solution format.
   */
  public static final VRPSolutionFormat INSTANCE = new VRPSolutionFormat();

  @Override
  public String getName() {
    return "vrp-tour";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
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
      throw new ParseException("Could not read VRP tour input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize VRP tour", e);
    }
  }
}