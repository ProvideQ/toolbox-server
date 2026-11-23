package edu.kit.provideq.formats.uvl;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.uvl.formats.UvlFormat;

import java.util.Set;

/**
 * Format family for feature-model inputs.
 */
public final class FeatureModelFamily implements FormatFamily {
  public static final FeatureModelFamily INSTANCE = new FeatureModelFamily();

  private FeatureModelFamily() {
  }

  @Override
  public String getName() {
    return "FeatureModel";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return UvlFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(UvlFormat.INSTANCE);
  }
}
