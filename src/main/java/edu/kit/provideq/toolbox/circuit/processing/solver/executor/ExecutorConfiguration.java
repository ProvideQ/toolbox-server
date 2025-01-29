package edu.kit.provideq.toolbox.circuit.processing.solver.executor;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ExecutorConfiguration {
  public static final ProblemType<String, ExecutionResult> EXECUTOR_CONFIG = new ProblemType<>(
      "circuit-processing-executor",
      String.class,
      ExecutionResult.class,
      null
  );

  @Bean
  ProblemManager<String, ExecutionResult> getExecutorProblemManager(
      ExecutionSolver executionSolver
  ) {
    return new ProblemManager<>(
        EXECUTOR_CONFIG,
        Set.of(
            executionSolver
        ),
        Set.of(new Problem<>(EXECUTOR_CONFIG))
    );
  }
}
