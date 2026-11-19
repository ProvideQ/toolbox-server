package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for dead-feature analysis solutions.
 */
public final class DeadFeatureSolutionFamily implements FormatFamily {
  public static final DeadFeatureSolutionFamily INSTANCE = new DeadFeatureSolutionFamily();

  private DeadFeatureSolutionFamily() {
  }

  @Override
  public String getName() {
    return "DeadFeatureSolution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return DeadFeatureSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(DeadFeatureSolutionFormat.INSTANCE);
  }
}
