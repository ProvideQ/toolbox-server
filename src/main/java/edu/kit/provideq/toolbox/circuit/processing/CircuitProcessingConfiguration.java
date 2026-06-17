package edu.kit.provideq.toolbox.circuit.processing;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.circuit.processing.results.Result;
import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToExecutionSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToMitigationSolver;
import edu.kit.provideq.toolbox.circuit.processing.solver.MoveToOptimizationSolver;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitProcessingConfiguration {
  public static final ProblemType<String, Result> CIRCUIT_PROCESSING = new ProblemType<>(
      "circuit-processing",
      "A quantum circuit processing problem that routes a QASM circuit through optimization, "
          + "error mitigation, or execution.",
      String.class,
      Result.class
  );

  @Bean
  ProblemManager<String, Result> getCircuitProcessingManager(
      ResourceProvider provider,
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
        loadExampleProblems(provider)
    );
  }

  private Set<Problem<String, Result>> loadExampleProblems(ResourceProvider provider) {
    try {
      String[] problemNames = new String[] {"bell-state.qasm", "cswap.qasm"};
      var problemSet = new HashSet<Problem<String, Result>>();
      for (var problemName : problemNames) {
        var problemStream = Objects.requireNonNull(
            getClass().getResourceAsStream(problemName), "Problem " + problemName + " not found");
        var problem = new Problem<>(CIRCUIT_PROCESSING);
        problem.setInput(provider.readStream(problemStream));
        problemSet.add(problem);
      }
      return problemSet;
    } catch (IOException e) {
      throw new MissingExampleException(CIRCUIT_PROCESSING, e);
    }
  }
}
