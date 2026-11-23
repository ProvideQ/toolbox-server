package edu.kit.provideq.toolbox.solvers.material_simulation.formats;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Set;

/**
 * Format family for molecule energy simulation results.
 */
public final class MoleculeEnergySolutionFamily implements FormatFamily {
  public static final MoleculeEnergySolutionFamily INSTANCE = new MoleculeEnergySolutionFamily();

  private MoleculeEnergySolutionFamily() {
  }

  @Override
  public String getName() {
    return "MoleculeEnergySolution";
  }

  @Override
  public Format<?> getCanonicalFormat() {
    return MoleculeEnergySolutionFormat.INSTANCE;
  }

  @Override
  public Set<Format<?>> getInitialFormats() {
    return Set.of(MoleculeEnergySolutionFormat.INSTANCE);
  }
}
