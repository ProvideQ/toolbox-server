package edu.kit.provideq.formats.gml;

import edu.kit.provideq.formats.core.ConversionRegistry;
import edu.kit.provideq.formats.core.FormatsToolbox;

/**
 * Bundles all GML module formats and property accessors for registration.
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
    registry.register(GmlFormat.INSTANCE);
  }

  /**
   * Registers all GML module property accessors with the given toolbox's property service.
   *
   * @param toolbox toolbox to register with
   */
  public static void registerProperties(FormatsToolbox toolbox) {
    toolbox.getPropertyService().register(new GmlNodeCountAccessor());
    toolbox.getPropertyService().register(new GmlEdgeCountAccessor());
  }
}
