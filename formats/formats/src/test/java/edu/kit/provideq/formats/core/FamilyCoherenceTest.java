package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.families.ContextBoundFormat;
import edu.kit.provideq.formats.core.families.FamilyCoherence;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link FamilyCoherence} invariant validation.
 */
class FamilyCoherenceTest {
  @Test
  void coherentFamilyPasses() {
    var registry = new ConversionRegistry();
    var formatA = new TestFormat("a");
    var formatB = new TestFormat("b");
    registry.registerFormat(formatA);
    registry.registerFormat(formatB);
    registry.registerBidirectional(
        new FuncConverter<>(formatA, formatB, v -> v),
        new FuncConverter<>(formatB, formatA, v -> v));
    registry.registerFormat(new TestFamily("family", formatA, Set.of(formatA, formatB)));
    registry.seal();

    assertDoesNotThrow(() -> FamilyCoherence.assertCoherent(registry));
  }

  @Test
  void incoherentFamilyFails() {
    var registry = new ConversionRegistry();
    var formatA = new TestFormat("a");
    var formatB = new TestFormat("b");
    registry.registerFormat(formatA);
    registry.registerFormat(formatB);
    registry.registerFormat(new TestFamily("family", formatA, Set.of(formatA, formatB)));
    registry.seal();

    var exception = assertThrows(IllegalStateException.class,
        () -> FamilyCoherence.assertCoherent(registry));
    assertTrue(exception.getMessage().contains("no conversion path from b to a"));
  }

  @Test
  void contextConnectedFamilyPasses() {
    var registry = new ConversionRegistry();
    var formatA = new TestFormat("a");
    var contextBound = new TestContextBoundFormat("context-bound");
    registry.registerFormat(formatA);
    registry.registerFormat(contextBound);
    registry.registerContextConverter(new TestContextConverter(formatA, contextBound));
    registry.registerFormat(
        new TestFamily("family", formatA, Set.of(formatA, contextBound)));
    registry.seal();

    // no context-free converter between a and context-bound exists, but the context
    // path satisfies the coherence requirement
    assertDoesNotThrow(() -> FamilyCoherence.assertCoherent(registry));
  }

  @Test
  void disconnectedContextBoundMemberFails() {
    var registry = new ConversionRegistry();
    var formatA = new TestFormat("a");
    var contextBound = new TestContextBoundFormat("context-bound");
    registry.registerFormat(formatA);
    registry.registerFormat(contextBound);
    // no converter at all between a and context-bound
    registry.registerFormat(
        new TestFamily("family", formatA, Set.of(formatA, contextBound)));
    registry.seal();

    var exception = assertThrows(IllegalStateException.class,
        () -> FamilyCoherence.assertCoherent(registry));
    assertTrue(exception.getMessage().contains("no conversion path"));
  }

  private record TestFamily(String name, Format<?> canonicalFormat, Set<Format<?>> formats)
      implements FormatFamily {
    @Override
    public String getName() {
      return name;
    }

    @Override
    public Format<?> getCanonicalFormat() {
      return canonicalFormat;
    }

    @Override
    public Set<Format<?>> getInitialFormats() {
      return formats;
    }
  }

  private record TestFormat(String name) implements Format<String> {
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
    public Class<String> getRepresentationClass() {
      return String.class;
    }

    @Override
    public String parse(java.io.Reader reader) {
      return "";
    }

    @Override
    public void serialize(String value, java.io.Writer writer) {
    }
  }

  private record TestContextBoundFormat(String name) implements Format<String>, ContextBoundFormat {
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
      return FormatType.SOLUTION;
    }

    @Override
    public Class<String> getRepresentationClass() {
      return String.class;
    }

    @Override
    public String parse(java.io.Reader reader) {
      return "";
    }

    @Override
    public void serialize(String value, java.io.Writer writer) {
    }
  }

  private record TestContextConverter(Format<String> source, Format<String> target)
      implements edu.kit.provideq.formats.core.converters.ContextConverter<
          String, String, String> {
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
      return value;
    }
  }
}
