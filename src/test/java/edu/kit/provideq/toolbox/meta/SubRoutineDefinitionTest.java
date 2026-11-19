package edu.kit.provideq.toolbox.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import edu.kit.provideq.formats.core.families.FormatFamily;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SubRoutineDefinition} factories.
 */
class SubRoutineDefinitionTest {
  private static final FormatFamily FAMILY = new TestFamily("family");
  private static final ProblemType<FormatFamily, FormatFamily> TYPE =
      new ProblemType<>("sub", "description", FAMILY, FAMILY);

  @Test
  void ofCreatesDefinitionWithTypeAndDescription() {
    var definition = SubRoutineDefinition.of(TYPE, "description", true);

    assertSame(TYPE, definition.type());
    assertEquals("description", definition.description());
    assertEquals(true, definition.isCalledOnlyOnce());
  }

  private record TestFamily(String name) implements FormatFamily {
    @Override
    public String getName() {
      return name;
    }

    @Override
    public edu.kit.provideq.formats.core.Format<?> getCanonicalFormat() {
      return null;
    }
  }
}
