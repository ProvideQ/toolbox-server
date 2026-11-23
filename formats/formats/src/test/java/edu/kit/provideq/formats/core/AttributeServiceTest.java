package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.attributes.AttributeNotFoundException;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.attributes.AttributeService;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.converters.Converter;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for attribute reading with conversion fallback.
 */
class AttributeServiceTest {
  private static final Format<Integer> FORMAT_C = new TestFormat<>("c", Map.of());

  @Test
  void attributeIsReadDirectly() throws FormatException {
    var formatA = formatWithCount("a");
    var attributeService = createAttributeService(formatA, null, null);

    var result = attributeService.read("hello", formatA, "count", Integer.class);

    assertEquals(5, result);
  }

  @Test
  void attributeFallsBackThroughConversion() throws FormatException {
    // only format B defines the attribute; A -> B appends "-b"
    var fallbackFormatA =
        new TestFormat<>("a-fallback", Map.<String, AttributeReader<String, ?>>of());
    var formatB = formatWithCount("b");
    var attributeService = createAttributeService(fallbackFormatA, formatB,
        new FuncConverter<>(fallbackFormatA, formatB,
            value -> value + "-b"));

    var result = attributeService.read("hi!", fallbackFormatA, "count", Integer.class);

    // The value is converted to format B ("hi!-b") before the attribute is read there.
    assertEquals(5, result);
  }

  @Test
  void unknownAttributeThrows() {
    var formatA = formatWithCount("a");
    var attributeService = createAttributeService(formatA, null, null);

    assertThrows(AttributeNotFoundException.class,
        () -> attributeService.read("x", formatA, "missing", Integer.class));
  }

  @Test
  void unreachableReaderThrowsAttributeNotFound() {
    var formatA = new TestFormat<>("a", Map.<String, AttributeReader<String, ?>>of());
    var registry = new ConversionRegistry();
    registry.registerFormat(formatA);
    registry.registerConverter(new FuncConverter<>(
        formatA, FORMAT_C, String::length));
    registry.seal();
    var attributeService = new AttributeService(registry, new ConversionService(registry));
    // attribute only defined on an isolated format without any conversion path
    var _ = formatWithCount("isolated");

    assertThrows(AttributeNotFoundException.class,
        () -> attributeService.read("x", formatA, "count", Integer.class));
  }

  @Test
  void attributeTypeMismatchThrows() {
    var formatA = formatWithCount("a");
    var attributeService = createAttributeService(formatA, null, null);

    assertThrows(FormatException.class,
        () -> attributeService.read("x", formatA, "count", String.class));
  }

  @Test
  void listedAttributesMatchDeclarations() {
    var formatA = formatWithCount("a");
    var attributeService = createAttributeService(formatA, null, null);

    assertEquals(List.of("count"), attributeService.getAttributeNames(formatA));
  }

  @Test
  void readAsStringConvertsTypedValues() throws FormatException {
    var formatA = formatWithCount("a");
    var attributeService = createAttributeService(formatA, null, null);

    assertEquals("5", attributeService.readAsString("hello", formatA, "count"));
  }

  @Test
  void lazyReaderAvoidsFullParse() throws FormatException {
    var lazyFormat = new TestFormat<>("lazy", Map.of(
        "count", AttributeReader.lazy(
            Integer.class, _ -> 42, _ -> {
              throw new AssertionError("full parse must not happen for lazy readers");
            })));
    var registry = new ConversionRegistry();
    registry.registerFormat(lazyFormat);
    registry.seal();
    var attributeService = new AttributeService(registry, new ConversionService(registry));

    var result = attributeService.readParsed(
        new StringReader("raw input"), lazyFormat, "count", Integer.class);

    assertEquals(42, result);
  }

  private Format<String> formatWithCount(String name) {
    return new TestFormat<>(name,
        Map.of("count", AttributeReader.of(Integer.class, String::length)));
  }

  private AttributeService createAttributeService(
      Format<String> formatA, Format<String> formatB, Converter<String, String> converter) {
    var registry = new ConversionRegistry();
    registry.registerFormat(formatA);
    if (formatB != null) {
      registry.registerFormat(formatB);
    }
    if (converter != null) {
      registry.registerConverter(converter);
    }
    registry.seal();

    return new AttributeService(registry, new ConversionService(registry));
  }

  private record TestFormat<T>(
      String name, Map<String, AttributeReader<T, ?>> readers) implements Format<T> {
    @Override
    public String getName() {
      return name;
    }

    @Override
    public Set<String> getAliases() {
      return Set.of();
    }

    @Override
    public Set<String> getFileExtensions() {
      return Set.of();
    }

    @Override
    public FormatType getType() {
      return FormatType.PROBLEM;
    }

    @Override
    public Map<String, AttributeReader<T, ?>> getAttributeReaders() {
      return readers;
    }

    @Override
    public Class<T> getRepresentationClass() {
      throw new UnsupportedOperationException();
    }

    @Override
    public T parse(java.io.Reader reader) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void serialize(T value, java.io.Writer writer) {
    }
  }
}
