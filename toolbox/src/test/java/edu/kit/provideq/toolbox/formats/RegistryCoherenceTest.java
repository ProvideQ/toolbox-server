package edu.kit.provideq.toolbox.formats;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.families.FamilyCoherence;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Validates the format setup of the whole toolbox: every problem type's input and result
 * families must be registered with the shared registry, their member formats must be
 * registered, and every registered family must be coherent (all member formats reachable
 * from each other via registered converters, possibly requiring problem context).
 *
 * <p>This guards against problem types declaring families whose formats cannot be
 * converted, which would fail at runtime when solvers or the API request conversions.
 */
@SpringBootTest
class RegistryCoherenceTest {
  @Autowired
  private ConversionRegistry conversionRegistry;

  @Autowired
  private ProblemManagerProvider problemManagerProvider;

  @Test
  void sharedRegistryFamiliesAreCoherent() {
    assertDoesNotThrow(() -> FamilyCoherence.assertCoherent(conversionRegistry));
  }

  @Test
  void problemTypeFamiliesAreRegistered() {
    for (var manager : problemManagerProvider.getProblemManagers()) {
      var type = manager.getType();
      assertFamilyRegistered(type.getId(), "input", type.getInputFamily());
      assertFamilyRegistered(type.getId(), "result", type.getResultFamily());
    }
  }

  private void assertFamilyRegistered(
      String problemTypeId,
      String familyRole,
      FormatFamily family) {
    assertSame(family, conversionRegistry.getFamily(family.getName()),
        "%s family of problem type %s is not registered"
            .formatted(familyRole, problemTypeId));

    Set<Format<?>> members = conversionRegistry.getFormats(family);
    assertFalse(members.isEmpty(),
        "%s family of problem type %s has no registered member formats"
            .formatted(familyRole, problemTypeId));

    var canonical = family.getCanonicalFormat();
    if (canonical != null) {
      assertTrue(members.contains(canonical),
          "%s family of problem type %s does not contain its canonical format %s"
              .formatted(familyRole, problemTypeId, canonical.getName()));
    }
  }
}
