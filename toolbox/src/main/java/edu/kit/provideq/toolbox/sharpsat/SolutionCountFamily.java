package edu.kit.provideq.toolbox.sharpsat;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for #SAT solutions.
 */
public final class SolutionCountFamily implements FormatFamily {
  public static final SolutionCountFamily INSTANCE = new SolutionCountFamily();

  private SolutionCountFamily() {
  }

  @Override
  public String getName() {
    return "SolutionCount";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return SolutionCountFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(SolutionCountFormat.INSTANCE);
  }
}
