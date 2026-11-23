package edu.kit.provideq.formats.knapsack;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.knapsack.formats.KnapsackFormat;
import edu.kit.provideq.formats.knapsack.formats.KnapsackSolutionFormat;

/**
 * Bundles all knapsack module formats for registration.
 */
public final class KnapsackFormats {
  private KnapsackFormats() {
  }

  /**
   * Registers all knapsack module formats with the given registry.
   *
   * @param registry registry to register with
   */
  public static void register(ConversionRegistry registry) {
    registry.registerFormat(KnapsackFormat.INSTANCE);
    registry.registerFormat(KnapsackSolutionFormat.INSTANCE);
  }
}
