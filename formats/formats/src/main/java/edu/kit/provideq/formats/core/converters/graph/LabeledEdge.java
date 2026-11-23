package edu.kit.provideq.formats.core.converters.graph;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Objects;

/**
 * An additional directed edge that is not a registered converter, e.g. a solver that
 * consumes its problem type's input family and produces its result family.
 *
 * <p>The endpoints may either be {@link Format}s (the edge connects two format nodes) or
 * {@link FormatFamily}s (the edge connects the family's subgraphs in the rendered diagram).
 *
 * @param source format or format family the edge starts at
 * @param target format or format family the edge points to
 * @param label  edge label, may be empty
 */
public record LabeledEdge(Object source, Object target, String label) {
  public LabeledEdge {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(target, "target");
    if (!(source instanceof Format || source instanceof FormatFamily)) {
      throw new IllegalArgumentException(
          "Edge source must be a Format or FormatFamily, but was: "
              + source.getClass().getName());
    }
    if (!(target instanceof Format || target instanceof FormatFamily)) {
      throw new IllegalArgumentException(
          "Edge target must be a Format or FormatFamily, but was: "
              + target.getClass().getName());
    }
    if ((source instanceof Format) != (target instanceof Format)) {
      throw new IllegalArgumentException(
          "Edge endpoints must both be Formats or both be FormatFamilies, but were: "
              + source.getClass().getName() + " and " + target.getClass().getName());
    }
    label = label == null ? "" : label;
  }
}
