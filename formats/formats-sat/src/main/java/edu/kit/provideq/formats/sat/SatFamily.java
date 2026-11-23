package edu.kit.provideq.formats.sat;

import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnfFormat;
import edu.kit.provideq.formats.sat.formats.logical.LogicalExpressionFormat;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * The format family of the satisfiability problem: all formats that can express
 * a SAT instance.
 *
 * <p>Membership is managed by the {@link ConversionRegistry};
 * this family is initially registered with the DIMACS CNF format (canonical) and the
 * logical expression format.
 */
public final class SatFamily implements FormatFamily {
  /**
   * Singleton instance of the SAT format family.
   */
  public static final SatFamily INSTANCE = new SatFamily();

  private SatFamily() {
  }

  @Override
  public String getName() {
    return "SAT";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return DimacsCnfFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(DimacsCnfFormat.INSTANCE, LogicalExpressionFormat.INSTANCE);
  }
}