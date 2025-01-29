package edu.kit.provideq.toolbox.circuit.processing.solver;

import edu.kit.provideq.toolbox.circuit.processing.CircuitProcessingConfiguration;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

public abstract class CircuitProcessingSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType<String, String> getProblemType() {
    return CircuitProcessingConfiguration.CIRCUIT_PROCESSING;
  }
}
