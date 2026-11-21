package edu.kit.provideq.formats.tsplib;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;

/**
 * Bundles all TSPLIB module formats for registration.
 */
public final class TsplibFormats {
  private TsplibFormats() {
  }

  /**
   * Registers all TSPLIB module formats with the given registry.
   *
   * @param registry registry to register with
   */
  public static void register(ConversionRegistry registry) {
    registry.registerFormat(TSPLIBFormat.INSTANCE);
    registry.registerFormat(TSPLIBSolutionFormat.INSTANCE);
    registry.registerFormat(VRPFormat.INSTANCE);
    registry.registerFormat(VRPSolutionFormat.INSTANCE);
    registry.registerConverter(VrpToTsplibConverter.INSTANCE);
  }
}
