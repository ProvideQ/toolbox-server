package edu.kit.provideq.toolbox.vrp;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.vrp.solvers.ClusterAndSolveVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.LkhVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.QrispVrpSolver;
import edu.kit.provideq.toolbox.tsp.solvers.QuboTspSolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@Configuration
public class VrpConfiguration {

  /**
   * A Capacitated Vehicle Routing Problem
   * Optimization Problem with the goal to find a minimal route for a given set of trucks and cities with demand.
   */
  public static final ProblemType<String, String> VRP = new ProblemType<>(
          "vrp",
          String.class,
          String.class
  );

  @Bean
  ProblemManager<String, String> getVrpManager(
          ResourceProvider resourceProvider,
          ClusterAndSolveVrpSolver clusterAndSolveVrpSolver,
          LkhVrpSolver lkhVrpSolver,
          QuboTspSolver quboTspSolver,
          QrispVrpSolver qrispVrpSolver) {
    return new ProblemManager<>(
            VRP,
            Set.of(clusterAndSolveVrpSolver,
                    lkhVrpSolver,
                    quboTspSolver,
                    qrispVrpSolver),
            loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
          ResourceProvider resourceProvider
  ) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("CMT1.vrp"),
          "Simple VRP CMT1 Problem unavailable!"
      );
      var problemTwoStream = Objects.requireNonNull(
          getClass().getResourceAsStream("SmallSample.vrp"),
          "Simple VRP SmallSample Problem unavailable!"
      );
      var problemThreeStream = Objects.requireNonNull(
              getClass().getResourceAsStream("VerySmallSampleForGrover.vrp"),
              "Very Small Problem unavailable");
      var problem = new Problem<>(VRP);
      var problemTwo = new Problem<>(VRP);
      var problemThree = new Problem<>(VRP);
      problem.setInput(resourceProvider.readStream(problemStream));
      problemTwo.setInput(resourceProvider.readStream(problemTwoStream));
      problemThree.setInput(resourceProvider.readStream(problemThreeStream));
      return Set.of(problem, problemTwo, problemThree);
    } catch (IOException e) {
      throw new MissingExampleException("Could not load example problems", e);
    }
  }
}
