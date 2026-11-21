package edu.kit.provideq.formats.lp;

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
 * The linear-programming problem format used for QUBO inputs.
 *
 * <p>The representation is the LP model text as accepted by the solver backends. The
 * format declares this structure so the data can be addressed deterministically;
 * attribute readers can be added as needed.
 *
 * <p>Example instance:
 * <pre>
 * Minimize
 *  obj: 0 + [ x1 * x2 ] / 2
 * Subject To
 * Binary
 *  x1 x2
 * End
 * </pre>
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record LPFormat() implements Format<String> {
  /**
   * Singleton instance of the LP format.
   */
  public static final LPFormat INSTANCE = new LPFormat();

  @Override
  public String getName() {
    return "lp";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("lp-format");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("lp");
  }

  @Override
  public FormatType getType() {
    return FormatType.PROBLEM;
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
      throw new ParseException("Could not read LP input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize LP model", e);
    }
  }
}