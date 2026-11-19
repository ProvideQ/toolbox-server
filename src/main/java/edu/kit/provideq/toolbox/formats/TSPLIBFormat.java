package edu.kit.provideq.toolbox.formats;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * The TSPLIB problem format used for TSP inputs.
 *
 * <p>The representation is the TSPLIB document text with {@code NAME}, {@code DIMENSION},
 * {@code EDGE_WEIGHT_TYPE} etc. header entries followed by the coordinate or weight
 * section. The {@code dimension} attribute reader extracts the {@code DIMENSION} header.
 *
 * <p>Example instance:
 * <pre>
 * NAME : tiny
 * TYPE : TSP
 * DIMENSION : 3
 * EDGE_WEIGHT_TYPE : EUC_2D
 * NODE_COORD_SECTION
 * 1 0.0 0.0
 * 2 1.0 0.0
 * 3 0.0 1.0
 * EOF
 * </pre>
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record TSPLIBFormat() implements Format<String> {
  /**
   * Singleton instance of the TSPLIB format.
   */
  public static final TSPLIBFormat INSTANCE = new TSPLIBFormat();

  @Override
  public Map<String, AttributeReader<String, ?>> getAttributeReaders() {
    return Map.of(
        "dimension", AttributeReader.of(
            String.class, text -> header(text, "DIMENSION")));
  }

  @Override
  public String getName() {
    return "tsplib";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("tsp");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("tsp");
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
      throw new ParseException("Could not read TSPLIB input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize TSPLIB document", e);
    }
  }

  private static String header(String text, String name) {
    return text.lines().filter(line -> line.startsWith(name)).findFirst()
        .map(line -> line.split(":")[1].trim()).orElse("");
  }
}