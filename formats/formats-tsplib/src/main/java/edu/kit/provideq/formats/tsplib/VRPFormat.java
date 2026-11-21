package edu.kit.provideq.formats.tsplib;

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
 * The VRP/TSPLIB-CVRP problem format used for VRP inputs.
 *
 * <p>The representation is the TSPLIB-CVRP document text with {@code NAME},
 * {@code DIMENSION}, {@code CAPACITY} etc. header entries followed by the coordinate and
 * demand sections. The {@code dimension} and {@code capacity} attribute readers extract
 * the corresponding headers.
 *
 * <p>Example instance:
 * <pre>
 * NAME : test
 * TYPE : CVRP
 * DIMENSION : 3
 * EDGE_WEIGHT_TYPE : EUC_2D
 * CAPACITY : 2
 * NODE_COORD_SECTION
 * 1 0.00000 0.00000
 * 2 1.00000 0.50000
 * 3 0.50000 -1.00000
 * DEMAND_SECTION
 * 1 0
 * 2 1
 * 3 2
 * DEPOT_SECTION
 * 1
 * -1
 * </pre>
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record VRPFormat() implements Format<String> {
  /**
   * Singleton instance of the VRP format.
   */
  public static final VRPFormat INSTANCE = new VRPFormat();

  @Override
  public Map<String, AttributeReader<String, ?>> getAttributeReaders() {
    return Map.of(
        "dimension", AttributeReader.of(String.class, text -> header(text, "DIMENSION")),
        "capacity", AttributeReader.of(String.class, text -> header(text, "CAPACITY")));
  }

  @Override
  public String getName() {
    return "vrp";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("vrp-format");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("vrp");
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
      throw new ParseException("Could not read VRP input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize VRP document", e);
    }
  }

  private static String header(String text, String name) {
    return text.lines().filter(line -> line.startsWith(name)).findFirst()
        .map(line -> line.split(":")[1].trim()).orElse("");
  }
}