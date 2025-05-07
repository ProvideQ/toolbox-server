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
 * {@link KnapsackConfiguration#KNAPSACK} solver using a Horowitz-Sahni implementation.
 */
@Component
public class PythonKnapsackSolver extends KnapsackSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public PythonKnapsackSolver(
          @Value("${path.custom.hs-knapsack}") String scriptPath,
          @Value("${venv.custom.hs-knapsack}") String venv,
          ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Horowitz-Sahni Knapsack";
  }

  @Override
  public String getDescription() {
    return "A solver for the Knapsack problem using the Horowitz-Sahni "
        + "branch and search algorithm.";
  }

  @Override
  public Mono<Solution<String>> solve(
          String input,
          SubRoutineResolver subRoutineResolver,
          SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH)
        .writeInputFile(input)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
