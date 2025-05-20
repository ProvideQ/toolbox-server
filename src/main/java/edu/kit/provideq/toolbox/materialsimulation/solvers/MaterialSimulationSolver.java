package edu.kit.provideq.toolbox.materialsimulation.solvers;

import edu.kit.provideq.toolbox.materialsimulation.MaterialSimulationConfiguration;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

public abstract class MaterialSimulationSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType<String, String> getProblemType() {
    return MaterialSimulationConfiguration.MATERIAL_SIMULATION;
  }
}
