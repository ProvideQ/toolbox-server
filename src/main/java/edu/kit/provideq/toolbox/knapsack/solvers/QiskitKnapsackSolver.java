package edu.kit.provideq.toolbox.knapsack.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.knapsack.KnapsackConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link KnapsackConfiguration#KNAPSACK} solver using a Qiskit implementation.
 */
@Component
public class QiskitKnapsackSolver extends KnapsackSolver {
  private final String knapsackPath;
  private final ApplicationContext context;

  @Autowired
  public QiskitKnapsackSolver(
      @Value("${qiskit.script.knapsack}") String knapsackPath,
      ApplicationContext context) {
    this.knapsackPath = knapsackPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Qiskit Knapsack";
  }

  @Override
  public String getDescription() {
    return "Solves the knapsack problem using Qiskit with QAOA.";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    var processResult = context
        .getBean(PythonProcessRunner.class, knapsackPath)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
