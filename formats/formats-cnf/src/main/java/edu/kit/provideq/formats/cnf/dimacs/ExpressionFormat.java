package edu.kit.provideq.formats.cnf.dimacs;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatKind;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.ParseException;
import edu.kit.provideq.formats.core.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

/**
 * The logical expression format for boolean formulas, e.g. {@code a and (b or not c)}.
 */
public record ExpressionFormat() implements Format<String> {
  /**
   * Singleton instance of the logical expression format.
   */
  public static final ExpressionFormat INSTANCE = new ExpressionFormat();

  @Override
  public String getName() {
    return "logical-expression";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("expression");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("expr");
  }

  @Override
  public FormatKind getKind() {
    return FormatKind.PROBLEM;
  }

  @Override
  public String parse(Reader reader) throws ParseException {
    try {
      return FormatReader.readAll(reader);
    } catch (IOException e) {
      throw new ParseException("Could not read logical expression input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize logical expression", e);
    }
  }
}
