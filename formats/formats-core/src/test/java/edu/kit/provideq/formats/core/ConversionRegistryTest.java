package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_B, true,
        value -> value + "-" + FORMAT_B.getName()));
    registry.seal();

    var service = new ConversionService(registry);
    var result = service.convert("test", FORMAT_A, FORMAT_B);

    assertEquals("test-b", result);
  }

  @Test
  void multiHopPathIsFound() throws FormatException {
    var registry = new ConversionRegistry();
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_B, true,
        value -> value + "-b"));
    registry.register(new FuncConverter<>(FORMAT_B, FORMAT_C, true,
        value -> value + "-c"));
    registry.seal();

    var service = new ConversionService(registry);
    var result = service.convert("x", FORMAT_A, FORMAT_C);

    assertEquals("x-b-c", result);
  }

  @Test
  void shortestPathIsPreferred() {
    var registry = new ConversionRegistry();
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_B, true,
        value -> value + "-b"));
    registry.register(new FuncConverter<>(FORMAT_B, FORMAT_C, true,
        value -> value + "-c"));
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_C, true,
        value -> value + "-c"));
    registry.seal();

    var path = registry.getPath(FORMAT_A, FORMAT_C);

    assertEquals(1, path.size());
  }

  @Test
  void losslessPathIsPreferredAmongEqualLength() {
    var registry = new ConversionRegistry();
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_C, false, value -> value));
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_C, true, value -> value));
    registry.seal();

    var path = registry.getPath(FORMAT_A, FORMAT_C);

    assertEquals(1, path.size());
    assertTrue(path.getFirst().isLossless());
  }

  @Test
  void missingRouteThrows() {
    var registry = new ConversionRegistry();
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_B, true, value -> value));
    registry.register(new FuncConverter<>(FORMAT_D, FORMAT_A, true, Object::toString));
    registry.seal();

    var service = new ConversionService(registry);

    assertThrows(RouteNotFoundException.class,
        () -> service.convert(1, FORMAT_D, FORMAT_C));
  }

  @Test
  void sealedRegistryRejectsRegistration() {
    var registry = new ConversionRegistry();
    registry.seal();

    assertThrows(IllegalStateException.class, () -> registry.register(FORMAT_A));
  }

  @Test
  void formatLookupByNameAndAlias() {
    var registry = new ConversionRegistry();
    registry.register(FORMAT_A);
    registry.register(FORMAT_B);

    assertEquals(FORMAT_A, registry.getFormat("a"));
    assertEquals(FORMAT_B, registry.getFormat("b"));
    assertNull(registry.getFormat("unknown"));
  }

  @Test
  void converterFailureIsPropagated() {
    var registry = new ConversionRegistry();
    registry.register(new FuncConverter<>(FORMAT_A, FORMAT_D, true,
        value -> {
          throw new FormatException("boom");
        }));
    registry.seal();

    var service = new ConversionService(registry);

    assertThrows(FormatException.class, () -> service.convert("x", FORMAT_A, FORMAT_D));
  }

  @Test
  void converterOfFactoryCreatesFunctionalConverter() throws FormatException {
    var converter = new FuncConverter<>(FORMAT_A, FORMAT_B, true, value -> value + "-of");

    assertEquals(FORMAT_A, converter.getSource());
    assertEquals(FORMAT_B, converter.getTarget());
    assertTrue(converter.isLossless());
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
