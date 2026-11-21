package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for format family registration and membership management in the registry.
 */
class FormatFamilyRegistryTest {
  private static final Format<String> FORMAT_A = new TestFormat("a");
  private static final Format<String> FORMAT_B = new TestFormat("b");
  private static final Format<String> FORMAT_C = new TestFormat("c");

  @Test
  void familyIsRegisteredWithInitialFormats() {
    var registry = new ConversionRegistry();
    var family = new TestFamily("family", FORMAT_A, Set.of(FORMAT_A, FORMAT_B));
    registry.registerFormat(family);

    assertSame(family, registry.getFamily("family"));
    assertEquals(Set.of(FORMAT_A, FORMAT_B), registry.getFormats(family));
  }

  @Test
  void familyRegistrationRegistersMemberFormats() {
    var registry = new ConversionRegistry();
    registry.registerFormat(new TestFamily("family", FORMAT_A, Set.of(FORMAT_A, FORMAT_B)));

    assertNotNull(registry.getFormat("a"));
    assertNotNull(registry.getFormat("b"));
  }

  @Test
  void formatsCanBeAddedToRegisteredFamilies() {
    var registry = new ConversionRegistry();
    var family = new TestFamily("family", FORMAT_A, Set.of(FORMAT_A));
    registry.registerFormat(family);
    registry.registerFormat(FORMAT_C);

    registry.addToFamily(family, FORMAT_C);

    assertEquals(Set.of(FORMAT_A, FORMAT_C), registry.getFormats(family));
  }

  @Test
  void addToFamilyRegistersUnknownFormats() {
    var registry = new ConversionRegistry();
    var family = new TestFamily("family", FORMAT_A, Set.of(FORMAT_A));
    registry.registerFormat(family);

    registry.addToFamily(family, FORMAT_C);

    assertNotNull(registry.getFormat("c"));
    assertEquals(Set.of(FORMAT_A, FORMAT_C), registry.getFormats(family));
  }

  @Test
  void addToUnregisteredFamilyThrows() {
    var registry = new ConversionRegistry();
    var family = new TestFamily("family", FORMAT_A, Set.of(FORMAT_A));

    assertThrows(IllegalArgumentException.class,
        () -> registry.addToFamily(family, FORMAT_C));
  }

  @Test
  void duplicateFamilyNameThrows() {
    var registry = new ConversionRegistry();
    registry.registerFormat(new TestFamily("family", FORMAT_A, Set.of(FORMAT_A)));

    assertThrows(IllegalStateException.class,
        () -> registry.registerFormat(new TestFamily("family", FORMAT_B, Set.of(FORMAT_B))));
  }

  @Test
  void familiesAreListed() {
    var registry = new ConversionRegistry();
    var family = new TestFamily("family", FORMAT_A, Set.of(FORMAT_A));
    registry.registerFormat(family);

    assertEquals(1, registry.getFamilies().size());
    assertSame(family, registry.getFamilies().iterator().next());
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
}
