package edu.kit.provideq.formats.core;

import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.RouteNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A value together with the format it is expressed in.
 *
 * <p>FormattedValue combines the family→format→value hierarchy: it carries a value
 * (text or parsed object) and the format it is expressed in, and can derive any other
 * representation for which the conversion graph provides a path. Derived representations
 * are cached, so repeated pulls of the same format are free.
 *
 * <p>Use {@link #of(Object, Format)} or {@link #parse(String, Format)} to create values;
 * both guarantee at compile time that the value matches the format's representation.
 *
 * @param <T> representation type of the value in its current format
 */
public final class FormattedValue<T> {
  private final T value;
  private final Format<T> format;
  private final Map<Format<?>, Object> cache = new HashMap<>();

  private FormattedValue(T value, Format<T> format) {
    this.value = Objects.requireNonNull(value, "value");
    this.format = Objects.requireNonNull(format, "format");
  }

  /**
   * Wraps a value that is already expressed in the given format.
   *
   * @param value value in the format's representation
   * @param format format the value is expressed in
   * @param <T> representation type
   * @return the formatted value
   */
  public static <T> FormattedValue<T> of(T value, Format<T> format) {
    return new FormattedValue<>(value, format);
  }

  /**
   * Parses the given text as the given format.
   *
   * @param text textual representation to parse
   * @param format format to parse the text as
   * @param <T> representation type
   * @return the formatted value
   * @throws ParseException if the text could not be parsed
   */
  public static <T> FormattedValue<T> parse(String text, Format<T> format) throws ParseException {
    return new FormattedValue<>(format.parse(text), format);
  }

  /**
   * Returns the format this value is currently expressed in.
   */
  public Format<T> getFormat() {
    return format;
  }

  /**
   * Returns the value in its current representation.
   */
  public T getValue() {
    return value;
  }

  /**
   * Returns the value in the given format's representation.
   * Returns the value directly if it is already in this format.
   * Otherwise, converts it via the conversion graph and caches the result.
   *
   * @param format requested representation format
   * @param conversionService service used to derive the representation
   * @param <U> requested representation type
   * @return the value in the requested representation
   * @throws RouteNotFoundException if no conversion path exists
   * @throws FormatException if a conversion step failed
   */
  @SuppressWarnings("unchecked")
  public <U> U as(Format<U> format, ConversionService conversionService) throws FormatException {
    if (this.format.equals(format)) {
      return (U) value;
    }

    if (cache.containsKey(format)) {
      return (U) cache.get(format);
    }

    if (conversionService == null) {
      throw new IllegalStateException(
          "Cannot derive representation without a conversion service");
    }

    var converted = conversionService.convert(value, this.format, format);
    cache.put(format, converted);
    return converted;
  }

  /**
   * Returns the formatted value in the given format's representation.
   * Returns the formatted value directly if it is already in this format.
   * Otherwise, converts it via the conversion graph and caches the result.
   *
   * @param format requested representation format
   * @param conversionService service used to derive the representation
   * @param <U> requested representation type
   * @return the formatted value in the requested representation
   * @throws RouteNotFoundException if no conversion path exists
   * @throws FormatException if a conversion step failed
   */
  public <U> FormattedValue<U> asFormatted(Format<U> format, ConversionService conversionService)
      throws FormatException {
    return FormattedValue.of(as(format, conversionService), format);
  }

  /**
   * Returns the value serialized to text using its current format.
   *
   * @return textual representation of the value
   */
  public String toString() {
    return format.serializeToString(value);
  }
}
