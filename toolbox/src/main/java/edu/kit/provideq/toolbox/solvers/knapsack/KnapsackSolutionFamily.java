package edu.kit.provideq.toolbox.solvers.knapsack;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.knapsack.KnapsackSolutionFormat;
import java.util.Set;

/**
 * Format family for knapsack solutions.
 */
public final class KnapsackSolutionFamily implements FormatFamily {
  public static final KnapsackSolutionFamily INSTANCE = new KnapsackSolutionFamily();

  private KnapsackSolutionFamily() {
  }

  @Override
  public String getName() {
    return "Knapsack-Solution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return KnapsackSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(KnapsackSolutionFormat.INSTANCE);
  }
}
