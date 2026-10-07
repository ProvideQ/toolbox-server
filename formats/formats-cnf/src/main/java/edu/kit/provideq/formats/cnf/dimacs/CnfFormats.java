package edu.kit.provideq.formats.cnf.dimacs;

import edu.kit.provideq.formats.core.ConversionRegistry;
import edu.kit.provideq.formats.core.FormatsToolbox;

/**
 * Bundles all CNF module formats, converters and property accessors for registration.
 */
public final class CnfFormats {
  private CnfFormats() {
  }

  /**
   * Registers all CNF module formats and converters with the given registry.
   *
   * @param registry registry to register with
   */
  public static void register(ConversionRegistry registry) {
    registry.register(DimacsCnfFormat.INSTANCE);
    registry.register(ExpressionFormat.INSTANCE);
    registry.register(ExpressionToDimacsCnfConverter.INSTANCE);
  }

  /**
   * Registers all CNF module property accessors with the given toolbox's property service.
   *
   * @param toolbox toolbox to register with
   */
  public static void registerProperties(FormatsToolbox toolbox) {
    toolbox.getPropertyService().register(new CnfVariableCountAccessor());
    toolbox.getPropertyService().register(new CnfClauseCountAccessor());
  }
}
