package edu.kit.provideq.formats.core;

import java.io.Reader;
import java.util.Objects;

/**
 * Factory for the library's services over a shared registry.
 *
 * <p>Typical usage:
 * <pre>{@code
 *   var registry = new ConversionRegistry();
 *   registry.register(DimacsCnfFormat.INSTANCE);
 *   registry.register(new ExpressionToDimacsCnfConverter(...));
 *   var toolbox = new FormatsToolbox(registry);
 * }</pre>
 */
public class FormatsToolbox {
  private final ConversionRegistry registry;
  private final ConversionService conversionService;
  private final PropertyService propertyService;

  /**
   * Creates a toolbox over the given registry.
   *
   * @param registry registry with registered formats and converters
   */
  public FormatsToolbox(ConversionRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.conversionService = new ConversionService(registry);
    this.propertyService = new PropertyService(registry, conversionService);
  }

  /**
   * Returns the registry this toolbox operates on.
   */
  public ConversionRegistry getRegistry() {
    return registry;
  }

  /**
   * Returns the conversion service.
   */
  public ConversionService getConversionService() {
    return conversionService;
  }

  /**
   * Returns the property service.
   */
  public PropertyService getPropertyService() {
    return propertyService;
  }

  /**
   * Convenience: converts a value between two formats by name.
   */
  public <A, B> B convert(A value, String from, String to) throws FormatException {
    return conversionService.convert(value, from, to);
  }

  /**
   * Convenience: reads a property from a value in the given format.
   */
  public <T, V> V readProperty(T value, Format<T> format, String property, Class<V> type)
      throws FormatException {
    return propertyService.read(value, format, property, type);
  }

  /**
   * Convenience: reads a property directly from input in the given format.
   */
  public <V> V readProperty(Reader reader, Format<?> format, String property, Class<V> type)
      throws FormatException {
    return propertyService.readParsed(reader, format, property, type);
  }
}
