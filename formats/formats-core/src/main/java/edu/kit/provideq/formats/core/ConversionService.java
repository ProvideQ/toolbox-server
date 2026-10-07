package edu.kit.provideq.formats.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Executes conversions between formats using the pre-computed paths of a
 * {@link ConversionRegistry}.
 */
public class ConversionService {
  private final ConversionRegistry registry;

  /**
   * Creates a conversion service backed by the given registry.
   *
   * @param registry registry with registered formats and converters
   */
  public ConversionService(ConversionRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  /**
   * Converts a value between formats by format reference.
   *
   * @param value value in the source representation
   * @param from source format
   * @param to target format
   * @param <A> source representation type
   * @param <B> target representation type
   * @return converted value
   * @throws RouteNotFoundException if no conversion path exists
   * @throws FormatException if a conversion step failed
   */
  @SuppressWarnings("unchecked")
  public <A, B> B convert(A value, Format<A> from, Format<B> to) throws FormatException {
    var path = registry.getPath(from, to);
    if (path == null) {
      throw new RouteNotFoundException(
          "No conversion path from %s to %s".formatted(from, to));
    }

    Object current = value;
    for (Converter<?, ?> converter : path) {
      current = apply((Converter<Object, Object>) converter, current);
    }

    return (B) current;
  }

  /**
   * Converts a value between formats by format name/alias.
   */
  public <A, B> B convert(A value, String fromName, String toName) throws FormatException {
    var from = registry.<A>getFormat(fromName);
    var to = registry.<B>getFormat(toName);
    if (from == null || to == null) {
      throw new RouteNotFoundException(
          "Unknown format: %s".formatted(from == null ? fromName : toName));
    }

    return convert(value, from, to);
  }

  /**
   * Serializes a value in one format and parses it as another format,
   * going through the textual representation. Requires that {@code to} can parse
   * what {@code from} serializes; only sensible when the two formats share the same text.
   */
  public <A, B> B roundTripThroughText(A value, Format<A> from, Format<B> to)
      throws FormatException {
    var text = from.serializeToString(value);
    return to.parse(text);
  }

  /**
   * Returns the conversion path between two formats, or {@code null} if none exists.
   */
  public List<Converter<?, ?>> getPath(Format<?> from, Format<?> to) {
    var path = registry.getPath(from, to);
    return path == null ? null : new ArrayList<>(path);
  }

  private Object apply(Converter<Object, Object> converter, Object value)
      throws FormatException {
    return converter.convert(value);
  }
}
