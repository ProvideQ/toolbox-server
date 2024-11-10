package edu.kit.provideq.toolbox.knapsack;

import edu.kit.provideq.toolbox.Bound;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.knapsack.solvers.PythonKnapsackSolver;
import edu.kit.provideq.toolbox.knapsack.solvers.QiskitKnapsackSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the Knapsack problem.
 */
@Configuration
public class KnapsackConfiguration {
  /**
   * An optimization problem:
   * For given items each with a weight and value, determine which items are part of a collection
   * where the total weight is less than or equal to a given limit
   * and the sum of values is as large as possible.
   */
  public static final ProblemType<String, String> KNAPSACK = new ProblemType<>(
      "knapsack",
        String.class,
        String.class,
        knapsackEstimator()
    );

  @Bean
  ProblemManager<String, String> getKnapsackManager(
          PythonKnapsackSolver pythonKnapsackSolver,
          QiskitKnapsackSolver qiskitKnapsackSolver,
          ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
            KNAPSACK,
            Set.of(pythonKnapsackSolver, qiskitKnapsackSolver),
            loadExampleProblems(resourceProvider)
    );
  }

  private static Function<String, Bound> knapsackEstimator() {
    throw new UnsupportedOperationException("Estimation of this problem type is not supported yet");
  }

  private Set<Problem<String, String>> loadExampleProblems(
        ResourceProvider resourceProvider
  ) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("4-items.txt"),
          "4-items example for Knapsack is unavailable!"
      );
      var problem = new Problem<>(KNAPSACK);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException("Could not load example problems", e);
    }
  }
}
