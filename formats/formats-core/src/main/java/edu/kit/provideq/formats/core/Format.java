package edu.kit.provideq.formats.core;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
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
  FormatKind getKind();

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
   * @throws SerializationException if the value could not be serialized
   */
  void serialize(T value, Writer writer) throws SerializationException;

  /**
   * Serializes the given object representation to a string.
   */
  default String serializeToString(T value) throws SerializationException {
    var writer = new StringWriter();
    serialize(value, writer);
    return writer.toString();
  }
}
