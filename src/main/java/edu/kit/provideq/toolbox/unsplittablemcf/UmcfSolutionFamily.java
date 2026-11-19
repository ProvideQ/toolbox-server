package edu.kit.provideq.toolbox.unsplittablemcf;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for unsplittable multi commodity flow solutions.
 */
public final class UmcfSolutionFamily implements FormatFamily {
  public static final UmcfSolutionFamily INSTANCE = new UmcfSolutionFamily();

  private UmcfSolutionFamily() {
  }

  @Override
  public String getName() {
    return "UMCF-Solution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return UmcfSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(UmcfSolutionFormat.INSTANCE);
  }
}
