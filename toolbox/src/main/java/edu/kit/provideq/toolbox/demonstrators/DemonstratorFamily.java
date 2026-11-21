package edu.kit.provideq.toolbox.demonstrators;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for demonstrator inputs.
 */
public final class DemonstratorFamily implements FormatFamily {
  public static final DemonstratorFamily INSTANCE = new DemonstratorFamily();

  private DemonstratorFamily() {
  }

  @Override
  public String getName() {
    return "Demonstrator";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return DemonstratorInputFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(DemonstratorInputFormat.INSTANCE);
  }
}
