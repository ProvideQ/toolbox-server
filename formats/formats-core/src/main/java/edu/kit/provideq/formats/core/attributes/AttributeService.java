package edu.kit.provideq.formats.core.attributes;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Reads typed attributes from formats.
 *
 * <p>Attribute readers are declared on the formats themselves (see
 * {@link Format#getAttributeReaders()}). If the requested format does not define a reader
 * for the attribute, the service converts the value (via the conversion graph) to the
 * nearest format that does define the reader.
 */
public class AttributeService {
  private final ConversionRegistry registry;
  private final ConversionService conversionService;

  /**
   * Creates an attribute service.
   *
   * @param registry registry with registered formats and converters
   * @param conversionService conversion service used for fallback conversions
   */
  public AttributeService(ConversionRegistry registry, ConversionService conversionService) {
    this.registry = Objects.requireNonNull(registry, "registry");
    this.conversionService = Objects.requireNonNull(conversionService, "conversionService");
  }

  /**
   * Reads a typed attribute from a value in the given format, using conversion fallback
   * to the nearest format defining the reader if needed.
   */
  public <T, V> V read(T value, Format<T> format, String attribute, Class<V> type)
      throws FormatException {
    var found = findReader(format, attribute);
    if (found == null) {
      throw new AttributeNotFoundException(
          "Unknown attribute %s for format %s".formatted(attribute, format));
    }

    Object attributeValue;
    if (found.sourceFormat().equals(format)) {
      // Directly read the attribute from the value in the requested format
      @SuppressWarnings("unchecked") // Type is guaranteed to be correct
      var reader = (AttributeReader<T, V>) found.reader();
      attributeValue = reader.read(value);
    } else {
      // Convert the value to the nearest format that defines the reader
      var converted = conversionService.convert(
          value, format, found.sourceFormat());
      @SuppressWarnings("unchecked") // Type is guaranteed to be the converted format's type
      var reader = (AttributeReader<Object, V>) found.reader();
      attributeValue = reader.read(converted);
    }

    return cast(attributeValue, type);
  }

  /**
   * Reads a typed attribute from a value, addressing the format by name/alias.
   */
  public <T, V> V read(T value, String formatName, String attribute, Class<V> type)
      throws FormatException {
    var format = registry.<T>getFormat(formatName);
    if (format == null) {
      throw new AttributeNotFoundException("Unknown format: %s".formatted(formatName));
    }

    return read(value, format, attribute, type);
  }

  /**
   * Reads an attribute directly from textual input in the given format. When the reader
   * provides a lazy reader, the input is scanned without building the full object;
   * otherwise the input is parsed first.
   */
  @SuppressWarnings("unchecked")
  public <V> V readParsed(Reader input, Format<?> format, String attribute, Class<V> type)
      throws FormatException {
    var foundReader = findReader(format, attribute);
    if (foundReader == null) {
      throw new AttributeNotFoundException(
          "Unknown attribute %s for format %s".formatted(attribute, format));
    }

    if (foundReader.reader().hasLazyReader()) {
      return cast(foundReader.reader().readLazy(input), type);
    }

    var parsedInput = format.parse(input);
    var reader = (AttributeReader<Object, V>) foundReader.reader();
    return cast(reader.read(parsedInput), type);
  }

  /**
   * Reads an attribute as its string representation, converting typed values
   * (e.g. {@code Integer}) as needed.
   */
  public <T> String readAsString(T value, Format<T> format, String attribute)
      throws FormatException {
    return readAsString(conversionService, value, format, attribute);
  }

  /**
   * Static entry point for reading an attribute as its string representation.
   */
  @SuppressWarnings("unchecked")
  public <T> String readAsString(
      ConversionService conversionService,
      T value,
      Format<T> format,
      String attribute) throws FormatException {
    var found = findReader(format, attribute);
    if (found == null) {
      throw new AttributeNotFoundException(
          "Unknown attribute %s for format %s".formatted(attribute, format));
    }

    Object attributeValue;
    if (found.sourceFormat().equals(format)) {
      var reader = (AttributeReader<T, Object>) found.reader();
      attributeValue = reader.read(value);
    } else {
      var converted = conversionService.convert(
          value, format, (Format<Object>) found.sourceFormat());
      var reader = (AttributeReader<Object, Object>) found.reader();
      attributeValue = reader.read(converted);
    }

    return String.valueOf(attributeValue);
  }

  /**
   * Lists the attribute names defined for a format, sorted alphabetically.
   */
  public List<String> getAttributeNames(Format<?> format) {
    return new ArrayList<>(new TreeMap<>(format.getAttributeReaders()).keySet());
  }

  /**
   * Finds the reader for an attribute on the given format.
   * If the format does not define one, searches formats reachable via the conversion graph.
   */
  private FoundReader findReader(Format<?> format, String attribute) {
    // Try to find the reader directly on the format first
    var ownReader = format.getAttributeReaders().get(attribute);
    if (ownReader != null) {
      return new FoundReader(ownReader, format);
    }

    // Find the nearest format that defines a reader for the attribute
    for (Format<?> candidate : getReachableFormats(registry, format)) {
      var candidateReader = candidate.getAttributeReaders().get(attribute);
      if (candidateReader != null) {
        return new FoundReader(candidateReader, candidate);
      }
    }

    return null;
  }

  /**
   * Returns all formats reachable from the given format, ordered by conversion path length.
   */
  private static List<Format<?>> getReachableFormats(
      ConversionRegistry registry,
      Format<?> source) {
    var result = new ArrayList<Format<?>>();
    var pathMap = registry.getPathsFrom(source);

    pathMap.entrySet().stream()
        .sorted(Comparator.comparingInt(a -> a.getValue().steps().size()))
        .forEach(entry -> result.add(entry.getKey()));

    return result;
  }

  @SuppressWarnings("unchecked")
  private static <V> V cast(Object value, Class<V> type) throws FormatException {
    if (!type.isInstance(value)) {
      throw new FormatException(
          "Attribute has type %s but %s was requested".formatted(
              value == null ? "null" : value.getClass().getName(), type.getName()));
    }
    return (V) value;
  }

  private record FoundReader(AttributeReader<?, ?> reader, Format<?> sourceFormat) {
  }
}