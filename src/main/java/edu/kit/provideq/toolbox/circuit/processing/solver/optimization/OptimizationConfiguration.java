package edu.kit.provideq.toolbox.circuit.processing.solver.optimization;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.circuit.processing.results.Result;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OptimizationConfiguration {
  public static final ProblemType<String, Result> OPTIMIZATION_CONFIG = new ProblemType<>(
      "circuit-processing-optimization",
      "A quantum circuit optimization problem that reduces gate count or circuit depth of a "
          + "given QASM circuit.",
      String.class,
      Result.class
  );

  @Bean
  ProblemManager<String, Result> getOptimizationProblemManager(
      ResourceProvider provider,
      OptimizationSolver optimizationSolver
  ) {
    return new ProblemManager<>(
        OPTIMIZATION_CONFIG,
        Set.of(optimizationSolver),
        loadExampleProblems(provider)
    );
  }

  private Set<Problem<String, Result>> loadExampleProblems(ResourceProvider provider) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("../../bell-state.qasm"), "Problem bell-state.qasm not found");
      var problem = new Problem<>(OPTIMIZATION_CONFIG);
      problem.setInput(provider.readStream(problemStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(OPTIMIZATION_CONFIG, e);
    }
  }
}
