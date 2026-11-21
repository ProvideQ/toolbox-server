package edu.kit.provideq.formats.ufl;

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
 * The UVL feature model format.
 *
 * <p>This format is textual: parsing validates the model via FeatureIDE when a converter
 * requires it. The format itself only transports the raw UVL text.
 *
 * <p>Example instance:
 * <pre>
 * namespace Sandwich
 *
 * features
 *     Sandwich
 *         mandatory
 *             Bread
 *         optional
 *             Cheese
 *                 alternative
 *                     Gouda
 *                     Toast
 * </pre>
 */
public record UvlFormat() implements Format<String> {
  /**
   * Singleton instance of the UVL format.
   */
  public static final UvlFormat INSTANCE = new UvlFormat();

  @Override
  public String getName() {
    return "uvl";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("uvl-feature-model");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("uvl");
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
      throw new ParseException("Could not read UVL input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize UVL", e);
    }
  }
}