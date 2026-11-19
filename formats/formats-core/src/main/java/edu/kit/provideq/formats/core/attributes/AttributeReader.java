package edu.kit.provideq.formats.core.attributes;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import java.io.Reader;
import java.util.Objects;

/**
 * A named, typed attribute reader for a format.
 *
 * <p>Attribute readers read out a single parameter (e.g. {@code variable_count},
 * {@code dimension}) from a format's representation. They are declared directly on the
 * format that owns the structure via {@link Format#getAttributeReaders()}.
 *
 * <p>A reader may additionally provide a lazy reader via {@link #lazy} that reads the
 * attribute directly from raw input without building the full object representation.
 *
 * @param <T> format's object representation
 * @param <V> attribute value type
 */
public final class AttributeReader<T, V> {
  private final Class<V> type;
  private final ValueFunction<T, V> reader;
  private final LazyValueReader<V> lazyReader;

  private AttributeReader(
      Class<V> type,
      ValueFunction<T, V> reader,
      LazyValueReader<V> lazyReader) {
    this.type = Objects.requireNonNull(type, "type");
    this.reader = Objects.requireNonNull(reader, "reader");
    this.lazyReader = lazyReader;
  }

  /**
   * Creates an attribute reader that reads the attribute from the parsed object
   * representation.
   *
   * @param type   attribute value type
   * @param reader read function over the format's representation
   * @param <T>    format's object representation
   * @param <V>    attribute value type
   * @return the attribute reader
   */
  public static <T, V> AttributeReader<T, V> of(
      Class<V> type, ValueFunction<T, V> reader) {
    return new AttributeReader<>(type, reader, null);
  }

  /**
   * Creates an attribute reader that can additionally read the attribute directly from
   * raw input without building the full object representation.
   *
   * @param type       attribute value type
   * @param lazyReader read function over the raw input
   * @param reader     read function over the format's representation
   * @param <T>        format's object representation
   * @param <V>        attribute value type
   * @return the attribute reader
   */
  public static <T, V> AttributeReader<T, V> lazy(
      Class<V> type, LazyValueReader<V> lazyReader, ValueFunction<T, V> reader) {
    return new AttributeReader<>(type, reader, Objects.requireNonNull(lazyReader));
  }

  /**
   * Returns the attribute value type.
   */
  public Class<V> getType() {
    return type;
  }

  /**
   * Returns whether this reader can read the attribute directly from raw input.
   */
  public boolean hasLazyReader() {
    return lazyReader != null;
  }

  /**
   * Reads the attribute from the already parsed object representation.
   *
   * @param value value in the format's representation
   * @return attribute value
   * @throws FormatException if the attribute could not be read
   */
  public V read(T value) throws FormatException {
    return reader.apply(value);
  }

  /**
   * Reads the attribute directly from raw input without building the full object
   * representation.
   *
   * @param input raw input
   * @return attribute value
   * @throws FormatException if the attribute could not be read
   * @throws IllegalStateException if this reader has no lazy reader
   */
  public V readLazy(Reader input) throws FormatException {
    if (lazyReader == null) {
      throw new IllegalStateException("Attribute reader has no lazy reader");
    }

    return lazyReader.read(input);
  }

  /**
   * A read function over a format's object representation that may throw a
   * {@link FormatException}.
   *
   * @param <T> format's object representation
   * @param <V> attribute value type
   */
  @FunctionalInterface
  public interface ValueFunction<T, V> {
    V apply(T value) throws FormatException;
  }

  /**
   * A read function over raw input that may throw a {@link FormatException}.
   *
   * @param <V> attribute value type
   */
  @FunctionalInterface
  public interface LazyValueReader<V> {
    V read(Reader input) throws FormatException;
  }
}
