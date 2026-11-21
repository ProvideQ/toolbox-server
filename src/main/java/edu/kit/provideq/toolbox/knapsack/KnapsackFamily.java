package edu.kit.provideq.toolbox.knapsack;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.knapsack.KnapsackFormat;
import java.util.Set;

/**
 * Format family for knapsack problem inputs.
 */
public final class KnapsackFamily implements FormatFamily {
  public static final KnapsackFamily INSTANCE = new KnapsackFamily();

  private KnapsackFamily() {
  }

  @Override
  public String getName() {
    return "Knapsack";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return KnapsackFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(KnapsackFormat.INSTANCE);
  }
}
