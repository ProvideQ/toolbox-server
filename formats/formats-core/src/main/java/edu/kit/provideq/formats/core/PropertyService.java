package edu.kit.provideq.formats.core;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads properties from formats.
 *
 * <p>If the requested format does not define an accessor for the property, the service
 * converts the value (via the pre-computed conversion tree) to the nearest format that
 * does define the accessor.
 */
public class PropertyService {
  private final ConversionRegistry registry;
  private final ConversionService conversionService;
  private final Map<Format<?>, Map<String, PropertyAccessor<?, ?>>> accessors = new HashMap<>();

  /**
   * Creates a property service.
   *
   * @param registry registry with registered formats and converters
   * @param conversionService conversion service used for fallback conversions
   */
  public PropertyService(ConversionRegistry registry, ConversionService conversionService) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.conversionService = Objects.requireNonNull(conversionService, "conversionService");
  }

  /**
   * Registers a property accessor for its format.
   *
   * @param accessor accessor to register
   */
  public <T, V> void register(PropertyAccessor<T, V> accessor) {
    Objects.requireNonNull(accessor, "accessor");
    accessors
        .computeIfAbsent(accessor.getFormat(), _ -> new HashMap<>())
        .put(accessor.getProperty(), accessor);
  }

  /**
   * Reads a property from a value in the given format, using conversion fallback if needed.
   */
  public <T, V> V read(T value, Format<T> format, String property, Class<V> type)
      throws FormatException {
    return readRaw(format, value, property, type);
  }

  /**
   * Reads a property from a value, addressing the format by name/alias.
   */
  public <T, V> V read(T value, String formatName, String property, Class<V> type)
      throws FormatException {
    var format = registry.<T>getFormat(formatName);
    if (format == null) {
      throw new PropertyNotFoundException("Unknown format: %s".formatted(formatName));
    }

    return read(value, format, property, type);
  }

  /**
   * Reads a property directly from textual input in the given format.
   * When a fallback conversion is needed, the input is parsed first.
   */
  @SuppressWarnings({"unchecked"})
  public <V> V readParsed(Reader reader, Format<?> format, String property, Class<V> type)
      throws FormatException {
    var accessor = findAccessor(format, property);
    if (accessor != null) {
      return readRawWithAccessor(accessor, reader, type);
    }

    // Parse the input, then fall back to conversion-based lookup.
    var parsed = format.parse(reader);
    return this.readRaw((Format<Object>) format, parsed, property, type);
  }

  @SuppressWarnings("unchecked")
  private <T, V> V readRaw(Format<T> format, Object value, String property, Class<V> type)
      throws FormatException {
    var accessor = findAccessor(format, property);
    if (accessor != null) {
      return readWithAccessor(accessor, format, (T) value, type);
    }

    throw new PropertyNotFoundException(
        "Unknown property %s for format %s".formatted(property, format));
  }

  /**
   * Lists the property names defined for a format.
   */
  public List<String> getProperties(Format<?> format) {
    var formatAccessors = accessors.get(format);
    if (formatAccessors == null) {
      return List.of();
    }
    return new ArrayList<>(formatAccessors.keySet());
  }

  /**
   * Finds the accessor for a property on the given format; if the format does not define
   * one, searches formats reachable via the conversion tree (BFS order, shortest path first).
   */
  private <T> PropertyAccessor<?, ?> findAccessor(Format<T> format, String property) {
    var formatAccessors = accessors.get(format);
    if (formatAccessors != null && formatAccessors.containsKey(property)) {
      return formatAccessors.get(property);
    }

    // Search the conversion tree: direct converters first, then multi-hop.
    for (Format<?> candidate : reachableFormats(format)) {
      var candidateAccessors = accessors.get(candidate);
      if (candidateAccessors != null && candidateAccessors.containsKey(property)) {
        return candidateAccessors.get(property);
      }
    }

    return null;
  }

  /**
   * Returns all formats reachable from the given format, ordered by conversion path length.
   */
  private List<Format<?>> reachableFormats(Format<?> source) {
    var result = new ArrayList<Format<?>>();
    var pathMap = registry.getPathsFrom(source);

    pathMap.entrySet().stream()
        .sorted(Comparator.comparingInt(a -> a.getValue().size()))
        .forEach(entry -> result.add(entry.getKey()));

    return result;
  }

  @SuppressWarnings("unchecked")
  private <T, V> V readWithAccessor(
      PropertyAccessor<?, ?> accessor, Format<T> format, T value, Class<V> type)
      throws FormatException {
    if (!accessor.getFormat().equals(format)) {
      // Convert to the accessor's format, then read.
      var converted = conversionService.convert(value, format, accessor.getFormat());
      return cast(((PropertyAccessor<Object, V>) accessor).read(converted), type);
    }

    return cast(((PropertyAccessor<T, V>) accessor).read(value), type);
  }

  @SuppressWarnings("unchecked")
  private <V> V readRawWithAccessor(PropertyAccessor<?, ?> accessor, java.io.Reader reader,
      Class<V> type) throws FormatException {
    return cast(((PropertyAccessor<Object, V>) accessor).readFromInput(reader), type);
  }

  @SuppressWarnings("unchecked")
  private <V> V cast(Object value, Class<V> type) throws FormatException {
    if (!type.isInstance(value)) {
      throw new FormatException(
          "Property has type %s but %s was requested".formatted(
              value == null ? "null" : value.getClass().getName(), type.getName()));
    }
    return (V) value;
  }
}
