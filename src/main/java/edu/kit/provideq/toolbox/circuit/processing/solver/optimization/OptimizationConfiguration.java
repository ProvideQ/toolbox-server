package edu.kit.provideq.toolbox.circuit.processing.solver.optimization;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class OptimizationConfiguration {
  public static final ProblemType<String, String> OPTIMIZATION_CONFIG = new ProblemType<>(
      "circuit-processing-optimization",
      String.class,
      String.class,
      null
  );

  @Bean
  ProblemManager<String, String> getOptimizationProblemManager(
      OptimizationSolver optimizationSolver
  ) {
    return new ProblemManager<>(
        OPTIMIZATION_CONFIG,
        Set.of(
            optimizationSolver
        ),
        Set.of(new Problem<>(OPTIMIZATION_CONFIG))
    );
  }
}
