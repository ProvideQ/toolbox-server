package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.RouteNotFoundException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FormattedValue}: lazy representation derivation and caching.
 */
class FormattedValueTest {
  private static final Format<String> FORMAT_A = new TestFormat<>("a");
  private static final Format<String> FORMAT_B = new TestFormat<>("b");
  private static final Format<Integer> FORMAT_C = new TestFormat<>("c", Integer.class);

  @Test
  void ofCarriesValueAndFormat() {
    var value = FormattedValue.of("hello", FORMAT_A);

    assertEquals("hello", value.getValue());
    assertSame(FORMAT_A, value.getFormat());
  }

  @Test
  void parseParsesThroughTheFormat() throws ParseException {
    var parsingFormat = new ParsingFormat();
    var value = FormattedValue.parse("hello", parsingFormat);

    assertEquals("parsed:hello", value.getValue());
    assertSame(parsingFormat, value.getFormat());
  }

  @Test
  void asWithSameFormatReturnsValueWithoutConversion() throws FormatException {
    var value = FormattedValue.of("hello", FORMAT_A);

    assertEquals("hello", value.as(FORMAT_A, null));
  }

  @Test
  void asConvertsViaRegistryAndCaches() throws FormatException {
    var registry = new ConversionRegistry();
    registry.registerFormat(FORMAT_A);
    registry.registerFormat(FORMAT_B);
    registry.registerConverter(new FuncConverter<>(FORMAT_A, FORMAT_B,
        v -> "converted:" + v));
    registry.seal();
    var formatted = FormattedValue.of("hello", FORMAT_A);
    var conversionService = new ConversionService(registry);

    assertEquals("converted:hello", formatted.as(FORMAT_B, conversionService));
    // second get is served from the cache (same instance)
    assertSame(formatted.as(FORMAT_B, conversionService),
        formatted.as(FORMAT_B, conversionService));
  }

  @Test
  void asWithoutPathThrows() {
    var registry = new ConversionRegistry();
    registry.registerFormat(FORMAT_A);
    registry.registerFormat(FORMAT_C);
    registry.seal();
    var formatted = FormattedValue.of("hello", FORMAT_A);
    var conversionService = new ConversionService(registry);

    assertThrows(RouteNotFoundException.class, () -> formatted.as(FORMAT_C, conversionService));
  }

  @Test
  void asWithoutConversionServiceThrows() {
    var formatted = FormattedValue.of("hello", FORMAT_A);

    var exception = assertThrows(IllegalStateException.class, () -> formatted.as(FORMAT_B, null));
    assertTrue(exception.getMessage().contains("conversion service"));
  }

  @Test
  void toStringSerializesViaTheFormat() {
    var value = FormattedValue.of("hello", new SerializingFormat());

    assertEquals("serialized:hello", value.toString());
  }

  private static class TestFormat<T> implements Format<T> {
    private final String name;
    private final Class<T> representationClass;

    TestFormat(String name) {
      this(name, null);
    }

    TestFormat(String name, Class<T> representationClass) {
      this.name = name;
      this.representationClass = representationClass;
    }

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
    public Class<T> getRepresentationClass() {
      return representationClass;
    }

    @Override
    public T parse(java.io.Reader reader) throws ParseException {
      return null;
    }

    @Override
    public void serialize(T value, java.io.Writer writer) throws SerializationException {
    }
  }

  private static class ParsingFormat extends TestFormat<String> {
    ParsingFormat() {
      super("a");
    }

    @Override
    public String parse(java.io.Reader reader) throws ParseException {
      try {
        return "parsed:" + FormatReader.readAll(reader);
      } catch (java.io.IOException e) {
        throw new ParseException("Could not read input", e);
      }
    }
  }

  private static class SerializingFormat extends TestFormat<String> {
    SerializingFormat() {
      super("a");
    }

    @Override
    public void serialize(String value, java.io.Writer writer) throws SerializationException {
      try {
        writer.write("serialized:" + value);
      } catch (java.io.IOException e) {
        throw new SerializationException("Could not serialize value", e);
      }
    }
  }
}
