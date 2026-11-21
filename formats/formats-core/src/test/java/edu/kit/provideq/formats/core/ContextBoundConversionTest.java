package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.converters.ContextConverter;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.RouteNotFoundException;
import java.io.Reader;
import java.io.Writer;
import org.junit.jupiter.api.Test;

/**
 * Tests for context converters: they participate in context-aware conversion paths, but
 * plain lookups never route through them.
 */
class ContextBoundConversionTest {
  private static final Format<String> FORMAT_A = new TestFormat<>("a", String.class);
  private static final Format<String> FORMAT_B = new TestFormat<>("b", String.class);
  private static final Format<String> FORMAT_C = new TestFormat<>("c", String.class);

  @Test
  void contextStepIsAppliedWithContext() throws FormatException {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        FORMAT_A, FORMAT_B, value -> value + "-b"));
    registry.registerContextConverter(new TestContextConverter(FORMAT_B, FORMAT_C));
    registry.seal();

    var service = new ConversionService(registry);
    var result = service.convert("x", FORMAT_A, FORMAT_C, "ctx");

    assertEquals("x-b-c(ctx)", result);
  }

  @Test
  void plainLookupDoesNotRouteThroughContextSteps() {
    var registry = new ConversionRegistry();
    registry.registerContextConverter(new TestContextConverter(FORMAT_A, FORMAT_B));
    registry.seal();

    // plain path lookup must not see the context edge
    assertNull(registry.getPath(FORMAT_A, FORMAT_B));
    var service = new ConversionService(registry);
    assertThrows(RouteNotFoundException.class,
        () -> service.convert("x", FORMAT_A, FORMAT_B));
  }

  @Test
  void wrongContextTypeIsRejected() {
    var registry = new ConversionRegistry();
    registry.registerContextConverter(new TestContextConverter(FORMAT_A, FORMAT_B));
    registry.seal();

    var service = new ConversionService(registry);
    // no path compatible with an Integer context exists
    assertThrows(RouteNotFoundException.class,
        () -> service.convert("x", FORMAT_A, FORMAT_B, 42));
  }

  @Test
  void contextPathIsFound() {
    var registry = new ConversionRegistry();
    registry.registerContextConverter(new TestContextConverter(FORMAT_A, FORMAT_B));
    registry.seal();

    var path = registry.getPath(FORMAT_A, FORMAT_B, "ctx");

    assertEquals(1, path.steps().size());
  }

  @Test
  void bestPathIsSelectedPerMode() throws FormatException {
    var registry = new ConversionRegistry();
    // plain route A -> B -> C (2 steps) and context route A -> C (1 step)
    registry.registerConverter(new FuncConverter<>(
        FORMAT_A, FORMAT_B, value -> value + "-b"));
    registry.registerConverter(new FuncConverter<>(
        FORMAT_B, FORMAT_C, value -> value + "-c"));
    registry.registerContextConverter(new TestContextConverter(FORMAT_A, FORMAT_C));
    registry.seal();

    var service = new ConversionService(registry);

    // plain lookup takes the 2-step context-free path
    var plainPath = registry.getPath(FORMAT_A, FORMAT_C);
    assertEquals(2, plainPath.steps().size());
    assertEquals("x-b-c", service.convert("x", FORMAT_A, FORMAT_C));

    // context lookup takes the shorter 1-step context path
    var contextPath = registry.getPath(FORMAT_A, FORMAT_C, "ctx");
    assertEquals(1, contextPath.steps().size());
    assertEquals("x-c(ctx)", service.convert("x", FORMAT_A, FORMAT_C, "ctx"));
  }

  private record TestContextConverter(Format<String> source, Format<String> target)
      implements ContextConverter<String, String, String> {
    @Override
    public Format<String> getSource() {
      return source;
    }

    @Override
    public Format<String> getTarget() {
      return target;
    }

    @Override
    public Class<String> getContextClass() {
      return String.class;
    }

    @Override
    public String convert(String value, String context) {
      return value + "-c(" + context + ")";
    }
  }

  private record TestFormat<T>(String name, Class<T> type) implements Format<T> {
    @Override
    public String getName() {
      return name;
    }

    @Override
    public java.util.Set<String> getAliases() {
      return java.util.Set.of();
    }

    @Override
    public java.util.Set<String> getFileExtensions() {
      return java.util.Set.of();
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
    public T parse(Reader reader) {
      return null;
    }

    @Override
    public void serialize(T value, Writer writer) {
    }
  }
}
