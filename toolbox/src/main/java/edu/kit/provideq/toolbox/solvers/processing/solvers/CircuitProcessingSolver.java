package edu.kit.provideq.toolbox.solvers.processing.solvers;

import edu.kit.provideq.toolbox.solvers.processing.CircuitProcessingConfiguration;
import edu.kit.provideq.toolbox.solvers.processing.QasmFamily;
import edu.kit.provideq.toolbox.solvers.processing.solvers.executor.ExecutionResultFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;

public abstract class CircuitProcessingSolver
    implements ProblemSolver<QasmFamily, ExecutionResultFamily> {
  public static final SubRoutineDefinition<QasmFamily, ExecutionResultFamily>
      CIRCUIT_PROCESSING_SUBROUTINE = SubRoutineDefinition.of(
          CircuitProcessingConfiguration.CIRCUIT_PROCESSING,
          "Creates a circuit processing solver",
          true
      );

  @Override
  public ProblemType<QasmFamily, ExecutionResultFamily> getProblemType() {
    return CircuitProcessingConfiguration.CIRCUIT_PROCESSING;
  }
}