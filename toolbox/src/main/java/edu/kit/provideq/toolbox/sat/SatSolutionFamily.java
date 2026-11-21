package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfSolutionFormat;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * The format family of satisfiability problem solutions: all formats that can express
 * a solution of a SAT instance.
 *
 * <p>Membership is managed by the {@link ConversionRegistry};
 * this family is initially registered with the DIMACS CNF solution format.
 */
public final class SatSolutionFamily implements FormatFamily {
  /**
   * Singleton instance of the SAT solution format family.
   */
  public static final SatSolutionFamily INSTANCE = new SatSolutionFamily();

  private SatSolutionFamily() {
  }

  @Override
  public String getName() {
    return "SAT-Solution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return DimacsCnfSolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(DimacsCnfSolutionFormat.INSTANCE);
  }
}