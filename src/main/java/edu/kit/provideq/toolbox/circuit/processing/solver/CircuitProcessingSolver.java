package edu.kit.provideq.toolbox.circuit.processing.solver;

import edu.kit.provideq.toolbox.circuit.processing.CircuitProcessingConfiguration;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;

public abstract class CircuitProcessingSolver implements ProblemSolver<String, String> {
  public static final SubRoutineDefinition<String, String> CIRCUIT_PROCESSING_SUBROUTINE =
      new SubRoutineDefinition<>(
          CircuitProcessingConfiguration.CIRCUIT_PROCESSING,
          "Creates a circuit processing solver"
      );

  @Override
  public ProblemType<String, String> getProblemType() {
    return CircuitProcessingConfiguration.CIRCUIT_PROCESSING;
  }
}
