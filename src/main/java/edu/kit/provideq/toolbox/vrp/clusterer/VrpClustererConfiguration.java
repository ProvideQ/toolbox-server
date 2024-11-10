package edu.kit.provideq.toolbox.vrp.clusterer;

import edu.kit.provideq.toolbox.Bound;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VrpClustererConfiguration {
  /**
   * A Configuration for Vehicle Routing Problem Clusterer.
   */
  public static final ProblemType<String, String> CLUSTER_VRP = new ProblemType<>(
      "cluster-vrp",
      String.class,
      String.class,
      clusterVrpEstimator()
  );

  @Bean
  ProblemManager<String, String> getClusterVrpManager(
      ResourceProvider resourceProvider,
      KmeansClusterer kmeans,
      TwoPhaseClusterer twoPhase) {
    return new ProblemManager<>(
        CLUSTER_VRP,
        Set.of(kmeans, twoPhase),
        loadExampleProblems(resourceProvider)
    );
  }

  private static Function<String, Bound> clusterVrpEstimator() {
    throw new UnsupportedOperationException("Estimation of this problem type is not supported yet");
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("../CMT1.vrp"),
          "Simple VRP CMT1 Problem unavailable!"
      );
      var problem = new Problem<>(CLUSTER_VRP);
      problem.setInput(resourceProvider.readStream(problemStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException("Could not load example problems", e);
    }
  }
}
