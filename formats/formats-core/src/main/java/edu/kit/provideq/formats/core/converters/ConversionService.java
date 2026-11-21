package edu.kit.provideq.formats.core.converters;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.RouteNotFoundException;
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
   * Returns the registry this service operates on.
   */
  public ConversionRegistry getRegistry() {
    return registry;
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
   * @throws RouteNotFoundException if no context-free conversion path exists
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
    for (Object step : path.steps()) {
      current = apply((Converter<Object, Object>) step, current);
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
   * Converts a value between formats, allowing the path to traverse context converters:
   * the best path compatible with the given context is selected, and the context object
   * is passed to the context steps' {@code convert(value, context)}.
   *
   * @param value value in the source representation
   * @param from source format
   * @param to target format
   * @param context the context object available for context steps
   * @param <A> source representation type
   * @param <B> target representation type
   * @return converted value
   * @throws RouteNotFoundException if no conversion path compatible with the context
   *         exists
   * @throws FormatException if a conversion step failed
   */
  @SuppressWarnings("unchecked")
  public <A, B> B convert(A value, Format<A> from, Format<B> to, Object context)
      throws FormatException {
    var path = registry.getPath(from, to, context);
    if (path == null) {
      throw new RouteNotFoundException(
          "No conversion path from %s to %s compatible with context %s".formatted(
              from, to, context == null ? "null" : context.getClass().getName()));
    }

    Object current = value;
    for (Object step : path.steps()) {
      if (step instanceof Converter<?, ?> converter) {
        current = apply((Converter<Object, Object>) converter, current);
      } else if (step instanceof ContextConverter<?, ?, ?> contextConverter) {
        current = applyContext(contextConverter, current, context);
      }
    }

    return (B) current;
  }

  /**
   * Validates the context against the converter's context class and applies the
   * conversion.
   */
  @SuppressWarnings("unchecked")
  private static <A, B, C> Object applyContext(
      ContextConverter<A, B, C> converter, Object value, Object context) throws FormatException {
    if (!converter.getContextClass().isInstance(context)) {
      throw new FormatException(
          "Conversion from %s to %s requires context of type %s, but was: %s".formatted(
              converter.getSource().getName(),
              converter.getTarget().getName(),
              converter.getContextClass().getName(),
              context == null ? "null" : context.getClass().getName()));
    }

    return converter.convert((A) value, (C) context);
  }

  /**
   * Returns the best context-free conversion path between two formats, or {@code null}
   * if none exists.
   */
  public ConversionPath getPath(Format<?> from, Format<?> to) {
    return registry.getPath(from, to);
  }

  private Object apply(Converter<Object, Object> converter, Object value)
      throws FormatException {
    return converter.convert(value);
  }
}
