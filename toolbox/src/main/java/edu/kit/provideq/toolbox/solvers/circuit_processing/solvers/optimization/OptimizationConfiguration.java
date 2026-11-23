package edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.optimization;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.qasm.QasmFamily;
import edu.kit.provideq.formats.qasm.formats.QasmFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.circuit_processing.formats.ExecutionResultFamily;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.optimization.solvers.OptimizationSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OptimizationConfiguration {
  public static final ProblemType<QasmFamily, ExecutionResultFamily>
      OPTIMIZATION_CONFIG =
      new ProblemType<>(
      "CircuitProcessingOptimization",
      "A quantum circuit optimization problem that reduces gate count or circuit depth of a "
          + "given QASM circuit.",
      QasmFamily.INSTANCE,
      ExecutionResultFamily.INSTANCE
  );

  @Bean
  ProblemManager<QasmFamily, ExecutionResultFamily> getOptimizationProblemManager(
      ResourceProvider provider,
      OptimizationSolver optimizationSolver
  ) {
    var manager = new ProblemManager<>(OPTIMIZATION_CONFIG, loadExampleProblems(provider));
    manager.registerSolvers(optimizationSolver);
    return manager;
  }

  private Set<Problem<QasmFamily, ExecutionResultFamily>> loadExampleProblems(
      ResourceProvider provider) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("../../bell-state.qasm"),
          "Problem bell-state.qasm not found");
      var problem = new Problem<>(OPTIMIZATION_CONFIG);
      problem.setInput(FormattedValue.of(
          provider.readStream(problemStream), QasmFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(OPTIMIZATION_CONFIG, e);
    }
  }
}
