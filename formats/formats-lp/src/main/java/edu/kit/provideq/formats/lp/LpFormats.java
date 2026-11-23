package edu.kit.provideq.formats.lp;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.lp.formats.LPFormat;
import edu.kit.provideq.formats.lp.formats.LPSolutionFormat;

/**
 * Bundles all LP module formats for registration.
 */
public final class LpFormats {
  private LpFormats() {
  }

  /**
   * Registers all LP module formats with the given registry.
   *
   * @param registry registry to register with
   */
  public static void register(ConversionRegistry registry) {
    registry.registerFormat(LPFormat.INSTANCE);
    registry.registerFormat(LPSolutionFormat.INSTANCE);
  }
}
