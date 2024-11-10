package edu.kit.provideq.toolbox.vrp;

import edu.kit.provideq.toolbox.Bound;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.tsp.solvers.QuboTspSolver;
import edu.kit.provideq.toolbox.vrp.solvers.ClusterAndSolveVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.LkhVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.QrispVrpSolver;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VrpConfiguration {

  /**
   * A Capacitated Vehicle Routing Problem
   * Optimization Problem with the goal to find a minimal route
   *     for a given set of trucks and cities with demand.
   */
  public static final ProblemType<String, String> VRP = new ProblemType<>(
      "vrp",
      String.class,
      String.class,
      vrpEstimator()
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

  private static Function<String, Bound> vrpEstimator() {
    throw new UnsupportedOperationException("Estimation of this problem type is not supported yet");
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      String[] problemNames = new String[] {
          "CMT1.vrp", "SmallSample.vrp", "VerySmallSampleForGrover.vrp"
      };

      var problemSet = new HashSet<Problem<String, String>>();
      for (var problemName : problemNames) {
        var problemStream = Objects.requireNonNull(getClass().getResourceAsStream(problemName),
            "Problem " + problemName + " not found");
        var problem = new Problem<>(VRP);
        problem.setInput(resourceProvider.readStream(problemStream));
        problemSet.add(problem);
      }
      return problemSet;
    } catch (IOException e) {
      throw new MissingExampleException("Could not load example problems", e);
    }
  }
}
