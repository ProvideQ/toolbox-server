package edu.kit.provideq.toolbox.circuit.processing.solver.optimization;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OptimizationConfiguration {
  public static final ProblemType<String, String> OPTIMIZATION_CONFIG = new ProblemType<>(
      "circuit-processing-optimization",
      String.class,
      String.class
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
