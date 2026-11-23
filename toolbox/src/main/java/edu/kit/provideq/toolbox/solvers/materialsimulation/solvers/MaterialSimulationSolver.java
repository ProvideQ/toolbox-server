package edu.kit.provideq.toolbox.solvers.materialsimulation.solvers;

import edu.kit.provideq.toolbox.solvers.materialsimulation.MaterialSimulationConfiguration;
import edu.kit.provideq.toolbox.solvers.materialsimulation.formats.MoleculeEnergySolutionFamily;
import edu.kit.provideq.formats.xyz.MoleculeFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

public abstract class MaterialSimulationSolver
    implements ProblemSolver<MoleculeFamily, MoleculeEnergySolutionFamily> {
  @Override
  public ProblemType<MoleculeFamily, MoleculeEnergySolutionFamily> getProblemType() {
    return MaterialSimulationConfiguration.MATERIAL_SIMULATION;
  }
}
