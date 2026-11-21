package edu.kit.provideq.formats.ufl;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;

/**
 * Bundles all UVL module formats and converters for registration.
 */
public final class UvlFormats {
  private UvlFormats() {
  }

  /**
   * Registers all UVL module formats and converters with the given registry.
   *
   * @param registry registry to register with
   */
  public static void register(ConversionRegistry registry) {
    registry.registerFormat(UvlFormat.INSTANCE);
    registry.registerConverter(UvlToDimacsCnfConverter.INSTANCE);
  }
}
