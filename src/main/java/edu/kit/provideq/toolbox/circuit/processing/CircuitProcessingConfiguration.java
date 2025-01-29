package edu.kit.provideq.toolbox.circuit.processing;

import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToExecutionSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToMitigationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToOptimizationSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitProcessingConfiguration {
  public static final ProblemType<String, String> CIRCUIT_PROCESSING = new ProblemType<>(
      "circuit-processing",
      String.class,
      String.class,
      null
  );

  @Bean
  ProblemManager<String, String> getCircuitProcessingManager(
      MoveToExecutionSolver moveToExecutionSolver,
      MoveToOptimizationSolver moveToOptimizationSolver,
      MoveToMitigationSolver moveToMitigationSolver
  ) {
    return new ProblemManager<>(
        CIRCUIT_PROCESSING,
        Set.of(
            moveToExecutionSolver,
            moveToOptimizationSolver,
            moveToMitigationSolver
        ),
        // TODO: dummy problem
        Set.of(new Problem<>(CIRCUIT_PROCESSING))
    );
  }
}
