package edu.kit.provideq.formats.gml;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;

/**
 * Bundles all GML module formats for registration.
 */
public final class GmlFormats {
  private GmlFormats() {
  }

  /**
   * Registers all GML module formats with the given registry.
   *
   * @param registry registry to register with
   */
  public static void register(ConversionRegistry registry) {
    registry.registerFormat(GmlFormat.INSTANCE);
  }
}
