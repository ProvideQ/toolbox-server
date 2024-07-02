package edu.kit.provideq.toolbox.tsp;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.tsp.solvers.LkhTspSolver;
import edu.kit.provideq.toolbox.tsp.solvers.QuboTspSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TspConfiguration {

  /**
   * A Traveling Sales Person Problem.
   * Optimization Problem with the goal of find an optimal route between a given set of connected cities.
   */
  public static final ProblemType<String, String> TSP = new ProblemType<>(
      "tsp",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getTspManager(
      ResourceProvider provider,
      QuboTspSolver quboTspSolver,
      LkhTspSolver lkhTspSolver
  ) {
    return new ProblemManager<>(TSP,
        Set.of(quboTspSolver, lkhTspSolver),
        loadExampleProblems(provider));
  }

  private Set<Problem<String, String>> loadExampleProblems(ResourceProvider provider) {
    try {
      var bigProblemStream = Objects.requireNonNull(getClass().getResourceAsStream("att48.tsp"));
      var bigProblem = new Problem<>(TSP);
      bigProblem.setInput(provider.readStream(bigProblemStream));

      var smallProblemStream = Objects.requireNonNull(getClass().getResourceAsStream("SmallSampleTSP.tsp"));
      var smallProblem = new Problem<>(TSP);
      smallProblem.setInput(provider.readStream(smallProblemStream));

      return Set.of(bigProblem, smallProblem);
    } catch (IOException e) {
      throw new MissingExampleException("Could not load example problems", e);
    }
  }
}
