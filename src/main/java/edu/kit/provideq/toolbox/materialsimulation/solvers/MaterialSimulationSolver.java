package edu.kit.provideq.toolbox.materialsimulation.solvers;

import edu.kit.provideq.toolbox.materialsimulation.MaterialSimulationConfiguration;
import edu.kit.provideq.toolbox.materialsimulation.MoleculeEnergySolutionFamily;
import edu.kit.provideq.toolbox.materialsimulation.MoleculeFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

public abstract class MaterialSimulationSolver
    implements ProblemSolver<MoleculeFamily, MoleculeEnergySolutionFamily> {
  @Override
  public ProblemType<MoleculeFamily, MoleculeEnergySolutionFamily> getProblemType() {
    return MaterialSimulationConfiguration.MATERIAL_SIMULATION;
  }
}
