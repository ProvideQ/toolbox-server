package edu.kit.provideq.toolbox.solvers.processing;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.qasm.QasmFamily;
import edu.kit.provideq.formats.qasm.formats.QasmFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.solvers.processing.solvers.MoveToExecutionSolver;
import edu.kit.provideq.toolbox.solvers.processing.solvers.MoveToOptimizationSolver;
import edu.kit.provideq.toolbox.solvers.processing.solvers.executor.ExecutionResultFamily;
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
  public static final ProblemType<QasmFamily, ExecutionResultFamily>
      CIRCUIT_PROCESSING =
      new ProblemType<>(
      "CircuitProcessing",
      "A quantum circuit processing problem that routes a QASM circuit through optimization, "
          + ", or execution.",
      QasmFamily.INSTANCE,
      ExecutionResultFamily.INSTANCE
  );

  @Bean
  QasmFamily registerQasmFamily(ConversionRegistry registry) {
    registry.registerFormat(QasmFamily.INSTANCE);
    return QasmFamily.INSTANCE;
  }

  @Bean
  ProblemManager<QasmFamily, ExecutionResultFamily> getCircuitProcessingManager(
      ResourceProvider provider,
      MoveToExecutionSolver moveToExecutionSolver,
      MoveToOptimizationSolver moveToOptimizationSolver
  ) {
    var manager = new ProblemManager<>(CIRCUIT_PROCESSING, loadExampleProblems(provider));
    manager.registerSolvers(
        moveToExecutionSolver,
        moveToOptimizationSolver
    );
    return manager;
  }

  private Set<Problem<QasmFamily, ExecutionResultFamily>> loadExampleProblems(
      ResourceProvider provider) {
    try {
      String[] problemNames = new String[] {"bell-state.qasm", "cswap.qasm"};
      var problemSet = new HashSet<Problem<QasmFamily, ExecutionResultFamily>>();
      for (var problemName : problemNames) {
        var problemStream = Objects.requireNonNull(
            getClass().getResourceAsStream(problemName), "Problem " + problemName + " not found");
        var problem = new Problem<>(CIRCUIT_PROCESSING);
        problem.setInput(FormattedValue.of(
            provider.readStream(problemStream), QasmFormat.INSTANCE));
        problemSet.add(problem);
      }
      return problemSet;
    } catch (IOException e) {
      throw new MissingExampleException(CIRCUIT_PROCESSING, e);
    }
  }
}
