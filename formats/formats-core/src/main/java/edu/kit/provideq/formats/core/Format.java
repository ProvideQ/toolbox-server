package edu.kit.provideq.formats.core;

import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * A format describes how to parse and serialize a specific problem or solution representation.
 *
 * <p>Formats are identified by their name; implementations should be immutable
 * value objects. The type parameter {@code T} is the object representation produced
 * by {@link #parse} and consumed by {@link #serialize}.
 *
 * @param <T> object representation of this format
 */
public interface Format<T> {
  /**
   * Returns the typed attribute readers supported directly by this representation.
   */
  default Map<String, AttributeReader<T, ?>> getAttributeReaders() {
    return Map.of();
  }

  /**
   * Returns the canonical name of this format, e.g. "dimacs-cnf".
   */
  String getName();

  /**
   * Returns alternative names for this format.
   */
  Set<String> getAliases();

  /**
   * Returns typical file extensions of this format.
   */
  Set<String> getFileExtensions();

  /**
   * Returns whether this is a problem or solution format.
   */
  FormatType getType();

  /**
   * Returns the Java class of the structured object that the representation produces on
   * {@link #parse}. For formats whose representation is the structured object itself, this
   * is the representation class; for formats whose representation is a {@link Document},
   * this is the class the document produces on {@link Document#parse()}.
   */
  Class<?> getRepresentationClass();

  /**
   * Parses the given input into the object representation of this format.
   *
   * @param reader input to parse
   * @return parsed object representation
   * @throws ParseException if the input could not be parsed
   */
  T parse(Reader reader) throws ParseException;

  /**
   * Parses the given string into the object representation of this format.
   */
  default T parse(String input) throws ParseException {
    return parse(new StringReader(input));
  }

  /**
   * Serializes the given object representation.
   *
   * @param value object representation to serialize
   * @param writer target of the serialized output
   * @throws SerializationException if there is an error during serialization
   */
  void serialize(T value, Writer writer) throws SerializationException;

  /**
   * Serializes the given object representation to a string.
   */
  default String serializeToString(T value) {
    var writer = new StringWriter();
    try {
      serialize(value, writer);
    } catch (SerializationException e) {
      throw new RuntimeException("Could not serialize value to string", e);
    }
    return writer.toString();
  }
}
