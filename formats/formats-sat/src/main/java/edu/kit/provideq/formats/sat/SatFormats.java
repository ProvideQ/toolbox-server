package edu.kit.provideq.formats.sat;

import edu.kit.provideq.formats.sat.converters.CnfToLogicalExpressionConverter;
import edu.kit.provideq.formats.sat.converters.LogicalExpressionToDimacsCnfConverter;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnfFormat;
import edu.kit.provideq.formats.sat.formats.logical.LogicalExpressionFormat;

/**
 * Bundles all CNF module formats and converters for registration.
 */
public final class SatFormats {
  private SatFormats() {
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
