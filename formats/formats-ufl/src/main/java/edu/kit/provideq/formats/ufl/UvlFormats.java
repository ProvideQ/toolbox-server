package edu.kit.provideq.formats.ufl;

import edu.kit.provideq.formats.core.ConversionRegistry;

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
    registry.register(UvlFormat.INSTANCE);
    registry.register(UvlToDimacsCnfConverter.INSTANCE);
  }
}
