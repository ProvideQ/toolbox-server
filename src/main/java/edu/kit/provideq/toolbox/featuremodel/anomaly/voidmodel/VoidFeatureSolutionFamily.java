package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for void-feature-model analysis solutions.
 */
public final class VoidFeatureSolutionFamily implements FormatFamily {
  public static final VoidFeatureSolutionFamily INSTANCE = new VoidFeatureSolutionFamily();

  private VoidFeatureSolutionFamily() {
  }

  @Override
  public String getName() {
    return "VoidFeatureSolution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return VoidFeatureSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(VoidFeatureSolutionFormat.INSTANCE);
  }
}
