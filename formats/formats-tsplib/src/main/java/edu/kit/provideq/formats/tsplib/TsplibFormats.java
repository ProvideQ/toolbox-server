package edu.kit.provideq.formats.tsplib;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.tsplib.converters.VrpToTsplibConverter;
import edu.kit.provideq.formats.tsplib.formats.TSPLIBFormat;
import edu.kit.provideq.formats.tsplib.formats.TSPLIBSolutionFormat;
import edu.kit.provideq.formats.tsplib.formats.VRPFormat;
import edu.kit.provideq.formats.tsplib.formats.VRPSolutionFormat;

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
