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
    Problem<String, String> demo = new Problem<>(CIRCUIT_PROCESSING);
    demo.setInput("""
        OPENQASM 2.0;
        include "qelib1.inc";
        qreg q[2];
        creg c[2];
        h q[0];
        cx q[0],q[1];
        measure q[0] -> c[0];
        measure q[1] -> c[1];""");
    Problem<String, String> secondDemo = new Problem<>(CIRCUIT_PROCESSING);
    secondDemo.setInput("""
        OPENQASM 2.0;
        include "qelib1.inc";
        qreg q[3];
        crz(0.5) q[0], q[1];
        t q[2];
        cswap q[2], q[0], q[1];""");
    return new ProblemManager<>(
        CIRCUIT_PROCESSING,
        Set.of(
            moveToExecutionSolver,
            moveToOptimizationSolver,
            moveToMitigationSolver
        ),
        Set.of(demo, secondDemo)
    );
  }
}
