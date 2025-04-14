package edu.kit.provideq.toolbox.quantum.material.simulation.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.quantum.material.simulation.QuantumMaterialSimulationConfiguration;

public abstract class QuantumMaterialSimulationSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType<String, String> getProblemType() {
    return QuantumMaterialSimulationConfiguration.QUANTUM_MATERIAL_SIMULATION;
  }
}
