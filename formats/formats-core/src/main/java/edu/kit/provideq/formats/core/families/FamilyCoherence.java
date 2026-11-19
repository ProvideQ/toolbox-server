package edu.kit.provideq.formats.core.families;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation of the family coherence invariant: for every registered family, a conversion
 * path must exist between every pair of member formats, possibly over multiple converter
 * edges, but not more than {@code MAX_PATH_LENGTH} steps.
 *
 * <p>Paths may require problem-instance context: a pair counts as connected if a
 * context-free path or a path requiring a single context object exists in either
 * direction (see {@link ContextBoundFormat}). The path index records the required context
 * type, so no context instance is needed for validation.
 */
public final class FamilyCoherence {
  private static final int MAX_PATH_LENGTH = 2;

  private FamilyCoherence() {
  }

  /**
   * Validates that all families of the given registry are coherent: every pair of member
   * formats has a conversion path between them, and the shortest such path does not
   * exceed {@code MAX_PATH_LENGTH} steps.
   *
   * @param registry registry with registered families
   * @throws IllegalStateException listing all violated member pairs, if any
   */
  public static void assertCoherent(ConversionRegistry registry) {
    var failures = new ArrayList<String>();

    for (var family : registry.getFamilies()) {
      var members = List.copyOf(registry.getFormats(family));
      for (var i = 0; i < members.size(); i++) {
        for (var j = 0; j < members.size(); j++) {
          if (i == j) {
            continue;
          }
          var from = members.get(i);
          var to = members.get(j);

          var pathList = registry.getPaths(from, to);
          if (pathList.isEmpty()) {
            // a pair also counts as connected when only the reverse direction has a
            // path (see ContextBoundFormat): context-connected members may only be
            // reachable in one direction
            if (registry.getPaths(to, from).isEmpty()) {
              failures.add("Family %s: no conversion path from %s to %s"
                  .formatted(family.getName(), from.getName(), to.getName()));
            }
            continue;
          }

          var shortest = pathList.getFirst();
          if (shortest.steps().size() > MAX_PATH_LENGTH) {
            failures.add("Family %s: conversion path from %s to %s is too long (%d steps)"
                .formatted(
                    family.getName(),
                    from.getName(),
                    to.getName(),
                    shortest.steps().size()));
          }
        }
      }
    }

    if (!failures.isEmpty()) {
      throw new IllegalStateException(
          "Family coherence violated:\n" + String.join("\n", failures));
    }
  }
}
