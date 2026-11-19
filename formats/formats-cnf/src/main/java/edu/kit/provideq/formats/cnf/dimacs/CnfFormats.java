package edu.kit.provideq.formats.cnf.dimacs;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;

/**
 * Bundles all CNF module formats and converters for registration.
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
    registry.registerFormat(DimacsCnfFormat.INSTANCE);
    registry.registerFormat(LogicalExpressionFormat.INSTANCE);
    registry.registerConverter(LogicalExpressionToDimacsCnfConverter.INSTANCE);
    registry.registerConverter(CnfToLogicalExpressionConverter.INSTANCE);
  }
}
