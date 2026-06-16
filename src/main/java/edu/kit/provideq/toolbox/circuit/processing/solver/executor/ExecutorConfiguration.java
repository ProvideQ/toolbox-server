package edu.kit.provideq.toolbox.circuit.processing.solver.executor;

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
public class ExecutorConfiguration {
  public static final ProblemType<String, Result> EXECUTOR_CONFIG = new ProblemType<>(
      "circuit-processing-executor",
      "A quantum circuit execution problem that runs a given QASM circuit on a quantum backend "
          + "and returns the measurement results.",
      String.class,
      Result.class
  );

  @Bean
  ProblemManager<String, Result> getExecutorProblemManager(
      ResourceProvider provider,
      ExecutionSolver executionSolver
  ) {
    return new ProblemManager<>(
        EXECUTOR_CONFIG,
        Set.of(executionSolver),
        loadExampleProblems(provider)
    );
  }

  private Set<Problem<String, Result>> loadExampleProblems(ResourceProvider provider) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("../../bell-state.qasm"),
          "Problem bell-state.qasm not found");
      var problem = new Problem<>(EXECUTOR_CONFIG);
      problem.setInput(provider.readStream(problemStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(EXECUTOR_CONFIG, e);
    }
  }
}
