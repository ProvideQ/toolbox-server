package edu.kit.provideq.formats.core;

import java.io.Reader;

/**
 * A named, typed property reader for a format.
 *
 * <p>Property accessors read out a single parameter (e.g. "dimension", "variable count")
 * from a format's representation. They may implement a cheap single-pass scan of the raw
 * input via {@link #readFromInput} instead of building the full object.
 *
 * @param <T> format's object representation
 * @param <V> property value type
 */
public interface PropertyAccessor<T, V> {
  /**
   * Returns the format this accessor reads from.
   */
  Format<T> getFormat();

  /**
   * Returns the name of the property this accessor reads, e.g. "dimension".
   */
  String getProperty();

  /**
   * Returns the value type of this property.
   */
  Class<V> getType();

  /**
   * Reads the property from the already parsed object representation.
   *
   * @param value value in the format's representation
   * @return property value
   * @throws FormatException if the property could not be read
   */
  V read(T value) throws FormatException;

  /**
   * Reads the property from the given string input.
   */
  default V read(String input) throws FormatException {
    return read(getFormat().parse(input));
  }

  /**
   * Reads the property directly from the input, possibly without building the full object.
   * The default implementation parses the full object first.
   *
   * @param reader raw input
   * @return property value
   * @throws FormatException if the property could not be read
   */
  default V readFromInput(Reader reader) throws FormatException {
    return read(getFormat().parse(reader));
  }
}
