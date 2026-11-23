package edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.qasm.QasmFamily;
import edu.kit.provideq.formats.qasm.formats.QasmFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.circuit_processing.formats.ExecutionResultFamily;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor.solvers.ExecutionSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfiguration {
  public static final ProblemType<QasmFamily, ExecutionResultFamily> EXECUTOR_CONFIG =
      new ProblemType<>(
      "CircuitProcessingExecutor",
      "A quantum circuit execution problem that runs a given QASM circuit on a quantum backend "
          + "and returns the measurement results.",
      QasmFamily.INSTANCE,
      ExecutionResultFamily.INSTANCE
  );

  @Bean
  ExecutionResultFamily registerExecutionResultFamily(ConversionRegistry registry) {
    registry.registerFormat(ExecutionResultFamily.INSTANCE);
    return ExecutionResultFamily.INSTANCE;
  }

  @Bean
  ProblemManager<QasmFamily, ExecutionResultFamily> getExecutorProblemManager(
      ResourceProvider provider,
      ExecutionSolver executionSolver
  ) {
    var manager = new ProblemManager<>(EXECUTOR_CONFIG, loadExampleProblems(provider));
    manager.registerSolvers(executionSolver);
    return manager;
  }

  private Set<Problem<QasmFamily, ExecutionResultFamily>> loadExampleProblems(
      ResourceProvider provider) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("../../bell-state.qasm"),
          "Problem bell-state.qasm not found");
      var problem = new Problem<>(EXECUTOR_CONFIG);
      problem.setInput(FormattedValue.of(
          provider.readStream(problemStream), QasmFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(EXECUTOR_CONFIG, e);
    }
  }
}
