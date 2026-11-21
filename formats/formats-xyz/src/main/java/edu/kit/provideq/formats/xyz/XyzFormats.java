package edu.kit.provideq.formats.xyz;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;

/**
 * Bundles all XYZ module formats for registration.
 */
public final class XyzFormats {
  private XyzFormats() {
  }

  /**
   * Registers all XYZ module formats with the given registry.
   *
   * @param registry registry to register with
   */
  public static void register(ConversionRegistry registry) {
    registry.registerFormat(XYZMoleculeFormat.INSTANCE);
  }
}
