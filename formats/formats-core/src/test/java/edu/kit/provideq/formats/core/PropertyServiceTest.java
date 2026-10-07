package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for property reading with conversion fallback.
 */
class PropertyServiceTest {
  private static final Format<String> FORMAT_A = new TestFormat<>("a");
  private static final Format<String> FORMAT_B = new TestFormat<>("b");
  private static final Format<Integer> FORMAT_C = new TestFormat<>("c");

  @Test
  void propertyIsReadDirectly() throws FormatException {
    var toolbox = createToolboxWithConverters();
    toolbox.getPropertyService().register(new CountAccessor(FORMAT_A, "count"));

    var result = toolbox.readProperty("hello", FORMAT_A, "count", Integer.class);

    assertEquals(5, result);
  }

  @Test
  void propertyFallsBackThroughConversion() throws FormatException {
    var toolbox = createToolboxWithConverters();
    // only format B defines the property; A -> B appends "-b"
    toolbox.getPropertyService().register(new CountAccessor(FORMAT_B, "count"));

    var result = toolbox.readProperty("hi!", FORMAT_A, "count", Integer.class);

    // The value is converted to format B ("hi!-b") before the property is read there.
    assertEquals(5, result);
  }

  @Test
  void unknownPropertyThrows() {
    var toolbox = createToolboxWithConverters();
    toolbox.getPropertyService().register(new CountAccessor(FORMAT_A, "count"));

    assertThrows(PropertyNotFoundException.class,
        () -> toolbox.readProperty("x", FORMAT_A, "missing", Integer.class));
  }

  @Test
  void unreachableAccessorThrowsPropertyNotFound() {
    var registry = new ConversionRegistry();
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_C, true, String::length));
    var toolbox = new FormatsToolbox(registry);
    // property only defined on an isolated format without any conversion path
    toolbox.getPropertyService().register(new CountAccessor(new TestFormat<>("isolated"),
        "count"));

    assertThrows(PropertyNotFoundException.class,
        () -> toolbox.readProperty("x", FORMAT_A, "count", Integer.class));
  }

  @Test
  void propertyTypeMismatchThrows() {
    var toolbox = createToolboxWithConverters();
    toolbox.getPropertyService().register(new CountAccessor(FORMAT_A, "count"));

    assertThrows(FormatException.class,
        () -> toolbox.readProperty("x", FORMAT_A, "count", String.class));
  }

  @Test
  void listedPropertiesMatchRegistrations() {
    var toolbox = createToolboxWithConverters();
    toolbox.getPropertyService().register(new CountAccessor(FORMAT_A, "count"));

    assertEquals(List.of("count"),
        toolbox.getPropertyService().getProperties(FORMAT_A));
  }

  private FormatsToolbox createToolboxWithConverters() {
    var registry = new ConversionRegistry();
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_B, true,
        value -> value + "-b"));
    registry.seal();

    return new FormatsToolbox(registry);
  }

  private record CountAccessor(Format<String> format, String property)
      implements PropertyAccessor<String, Integer> {
    @Override
    public Format<String> getFormat() {
      return format;
    }

    @Override
    public String getProperty() {
      return property;
    }

    @Override
    public Class<Integer> getType() {
      return Integer.class;
    }

    @Override
    public Integer read(String value) {
      return value.length();
    }
  }

  private record TestFormat<T>(String name) implements Format<T> {
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
    public FormatKind getKind() {
      return FormatKind.PROBLEM;
    }

    @Override
    public T parse(java.io.Reader reader) {
      return null;
    }

    @Override
    public void serialize(T value, java.io.Writer writer) {
    }
  }
}
