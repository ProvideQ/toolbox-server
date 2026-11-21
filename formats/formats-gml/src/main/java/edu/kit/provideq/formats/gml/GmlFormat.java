package edu.kit.provideq.formats.gml;

import edu.kit.provideq.formats.core.Document;
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
 * The GML graph format.
 *
 * <p>Example instance:
 * <pre>
 * graph [
 *   id 42
 *   node [
 *     id 1
 *     label "1"
 *   ]
 *   node [
 *     id 2
 *     label "2"
 *   ]
 *   edge [
 *     source 1
 *     target 2
 *     weight 5
 *   ]
 * ]
 * </pre>
 */
public record GmlFormat() implements Format<Document<Gml>> {
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
  public FormatType getType() {
    return FormatType.PROBLEM;
  }

  @Override
  public Class<Gml> getRepresentationClass() {
    return Gml.class;
  }

  @Override
  public Map<String, AttributeReader<Document<Gml>, ?>> getAttributeReaders() {
    return Map.of(
        "node_count", AttributeReader.of(Integer.class, gml -> gml.parse().nodes().size()),
        "edge_count", AttributeReader.of(Integer.class, gml -> gml.parse().edges().size()));
  }

  @Override
  public Document<Gml> parse(Reader reader) throws ParseException {
    try {
      // The returned document retains the raw input; it is parsed into the structured
      // GML lazily on Document.parse().
      return Document.of(FormatReader.readAll(reader), Gml::fromString);
    } catch (IOException e) {
      throw new ParseException("Could not read GML input", e);
    }
  }

  /**
   * Serializes a GML object to the given writer.
   * The format looks like this:
   * <pre>
   *   graph [
   *     node [
   *       id 1
   *       label "Node 1"
   *     ]
   *     node [
   *       id 2
   *       label "Node 2"
   *     ]
   *     edge [
   *       source 1
   *       target 2
   *     ]
   *   ]
   * </pre>
   *
   * @param value object representation to serialize
   * @param writer target of the serialized output
   * @throws SerializationException if there is an error during serialization
   */
  @Override
  public void serialize(Document<Gml> value, Writer writer) throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize GML", e);
    }
  }
}