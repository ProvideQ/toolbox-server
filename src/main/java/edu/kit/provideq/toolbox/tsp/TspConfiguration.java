package edu.kit.provideq.toolbox.tsp;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.tsp.solvers.LkhTspSolver;
import edu.kit.provideq.toolbox.tsp.solvers.QuboTspSolver;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TspConfiguration {

  /**
   * A Traveling Sales Person Problem.
   * Optimization Problem with the goal of find an optimal route
   *     between a given set of connected cities.
   */
  public static final ProblemType<String, String> TSP = new ProblemType<>(
      "TSP",
      "A Traveling Sales Person Problem. Optimization Problem with the goal of find "
          + "an optimal route between a given set of connected cities.",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getTspManager(
      ResourceProvider provider,
      QuboTspSolver quboTspSolver,
      LkhTspSolver lkhTspSolver
  ) {
    return new ProblemManager<>(
        TSP,
        Set.of(quboTspSolver, lkhTspSolver),
        loadExampleProblems(provider));
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider provider) {
    try {
      String[] problemNames = new String[] {
          "att48.tsp", "SmallSampleTSP.tsp", "VerySmallSampleTSP.tsp"
      };

      var problemSet = new HashSet<Problem<String, String>>();
      for (var problemName : problemNames) {
        var problemStream = Objects.requireNonNull(getClass().getResourceAsStream(problemName),
            "Problem " + problemName + " not found");
        var problem = new Problem<>(TSP);
        problem.setInput(provider.readStream(problemStream));
        problemSet.add(problem);
      }
      return problemSet;
    } catch (IOException e) {
      throw new MissingExampleException(TSP, e);
    }
  }
}
