package edu.kit.provideq.toolbox.unsplittablemcf;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for unsplittable multi commodity flow instance inputs.
 */
public final class UmcfFamily implements FormatFamily {
  public static final UmcfFamily INSTANCE = new UmcfFamily();

  private UmcfFamily() {
  }

  @Override
  public String getName() {
    return "UMCF";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return UmcfFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(UmcfFormat.INSTANCE);
  }
}
