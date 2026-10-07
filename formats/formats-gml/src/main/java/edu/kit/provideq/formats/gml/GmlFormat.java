package edu.kit.provideq.formats.gml;

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
 * The GML graph format.
 */
public record GmlFormat() implements Format<Gml> {
  /**
   * Singleton instance of the GML format.
   */
  public static final GmlFormat INSTANCE = new GmlFormat();

  @Override
  public String getName() {
    return "gml";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("gml");
  }

  @Override
  public FormatKind getKind() {
    return FormatKind.PROBLEM;
  }

  @Override
  public Gml parse(Reader reader) throws ParseException {
    try {
      return Gml.fromString(FormatReader.readAll(reader));
    } catch (IOException e) {
      throw new ParseException("Could not read GML input", e);
    }
  }

  @Override
  public void serialize(Gml value, Writer writer) throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize GML", e);
    }
  }
}
