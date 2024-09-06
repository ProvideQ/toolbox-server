package edu.kit.provideq.toolbox.knapsack.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.knapsack.KnapsackConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
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
  private final String knapsackPath;
  private final ApplicationContext context;

  @Autowired
  public PythonKnapsackSolver(
          @Value("${custom.hs_knapsack.directory}") String knapsackPath,
          ApplicationContext context) {
    this.knapsackPath = knapsackPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Horowitz-Sahni Knapsack";
  }

  @Override
  public Mono<Solution<String>> solve(
          String input,
          SubRoutineResolver subRoutineResolver,
          SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    var processResult = context
            .getBean(
                    PythonProcessRunner.class,
                    knapsackPath,
                    "knapsack.py")
            .addProblemFilePathToProcessCommand()
            .addSolutionFilePathToProcessCommand()
            .run(getProblemType(), solution.getId(), input);

    return Mono.just(processResult.applyTo(solution));
  }
}
