package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.RouteNotFoundException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the core conversion registry, routing and property reading.
 */
class ConversionRegistryTest {
  private static final Format<String> FORMAT_A = new TestFormat<>("a", String.class);
  private static final Format<String> FORMAT_B = new TestFormat<>("b", String.class);
  private static final Format<String> FORMAT_C = new TestFormat<>("c", String.class);
  private static final Format<Integer> FORMAT_D = new TestFormat<>("d", Integer.class);

  @Test
  void directPathIsFound() throws FormatException {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        FORMAT_A, FORMAT_B,
        value -> value + "-" + FORMAT_B.getName()));
    registry.seal();

    var service = new ConversionService(registry);
    var result = service.convert("test", FORMAT_A, FORMAT_B);

    assertEquals("test-b", result);
  }

  @Test
  void multiHopPathIsFound() throws FormatException {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        FORMAT_A, FORMAT_B,
        value -> value + "-b"));
    registry.registerConverter(new FuncConverter<>(
        FORMAT_B, FORMAT_C,
        value -> value + "-c"));
    registry.seal();

    var service = new ConversionService(registry);
    var result = service.convert("x", FORMAT_A, FORMAT_C);

    assertEquals("x-b-c", result);
  }

  @Test
  void shortestPathIsPreferred() {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        FORMAT_A, FORMAT_B,
        value -> value + "-b"));
    registry.registerConverter(new FuncConverter<>(
        FORMAT_B, FORMAT_C,
        value -> value + "-c"));
    registry.registerConverter(new FuncConverter<>(
        FORMAT_A, FORMAT_C,
        value -> value + "-c"));
    registry.seal();

    var path = registry.getPath(FORMAT_A, FORMAT_C);

    assertEquals(1, path.steps().size());
  }

  @Test
  void missingRouteThrows() {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        FORMAT_A, FORMAT_B,
        value -> value));
    registry.registerConverter(new FuncConverter<>(
        FORMAT_D, FORMAT_A,
        Object::toString));
    registry.seal();

    var service = new ConversionService(registry);

    assertThrows(RouteNotFoundException.class,
        () -> service.convert(1, FORMAT_D, FORMAT_C));
  }

  @Test
  void sealedRegistryRejectsRegistration() {
    var registry = new ConversionRegistry();
    registry.seal();

    assertThrows(IllegalStateException.class, () -> registry.registerFormat(FORMAT_A));
  }

  @Test
  void formatLookupByNameAndAlias() {
    var registry = new ConversionRegistry();
    registry.registerFormat(FORMAT_A);
    registry.registerFormat(FORMAT_B);

    assertEquals(FORMAT_A, registry.getFormat("a"));
    assertEquals(FORMAT_B, registry.getFormat("b"));
    assertNull(registry.getFormat("unknown"));
  }

  @Test
  void converterFailureIsPropagated() {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        FORMAT_A, FORMAT_D,
        _ -> {
          throw new FormatException("boom");
        }));
    registry.seal();

    var service = new ConversionService(registry);

    assertThrows(FormatException.class, () -> service.convert("x", FORMAT_A, FORMAT_D));
  }

  @Test
  void converterOfFactoryCreatesFunctionalConverter() throws FormatException {
    var converter = new FuncConverter<>(FORMAT_A, FORMAT_B,
        value -> value + "-of");

    assertEquals(FORMAT_A, converter.getSource());
    assertEquals(FORMAT_B, converter.getTarget());
    assertEquals("x-of", converter.convert("x"));
  }

  private record TestFormat<T>(String name, Class<T> type) implements Format<T> {
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
      return type;
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
