package edu.kit.provideq.toolbox.knapsack.solvers;

import edu.kit.provideq.toolbox.PythonProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PythonKnapsackSolver extends KnapsackSolver {
  private final String knapsackPath;
  private final ApplicationContext context;

  @Autowired
  public PythonKnapsackSolver(
          @Value("${python.directory}/knapsack") String knapsackPath,
          ApplicationContext context) {
    this.knapsackPath = knapsackPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Python Knapsack";
  }

  @Override
  public Mono<Solution<String>> solve(
          String input,
          SubRoutineResolver subRoutineResolver
  ) {
    var solution = new Solution<String>();

    var processResult = context
            .getBean(
                    PythonProcessRunner.class,
                    knapsackPath,
                    "knapsack.py")
            .run(getProblemType(), solution.getId(), input);

    return Mono.just(processResult.applyTo(solution));
  }
}
