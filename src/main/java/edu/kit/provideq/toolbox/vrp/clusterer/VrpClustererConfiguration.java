package edu.kit.provideq.toolbox.vrp.clusterer;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.tsplib.VRPFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.vrp.VrpFamily;
import edu.kit.provideq.toolbox.vrp.VrpSolutionFamily;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VrpClustererConfiguration {
  /**
   * A Configuration for Vehicle Routing Problem Clusterer.
   */
  public static final ProblemType<VrpFamily, VrpSolutionFamily> CLUSTER_VRP =
      new ProblemType<>(
      "ClusterVRP",
      "A Configuration for Vehicle Routing Problem Clusterer.",
      VrpFamily.INSTANCE,
      VrpSolutionFamily.INSTANCE
  );

  @Bean
  ProblemManager<VrpFamily, VrpSolutionFamily> getClusterVrpManager(
      ResourceProvider resourceProvider,
      KmeansClusterer kmeans,
      TwoPhaseClusterer twoPhase) {
    var manager = new ProblemManager<>(CLUSTER_VRP, loadExampleProblems(resourceProvider));
    manager.registerSolvers(kmeans, twoPhase);
    return manager;
  }

  private Set<Problem<VrpFamily, VrpSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("../CMT1.vrp"),
          "Simple VRP CMT1 Problem unavailable!"
      );
      var problem = new Problem<VrpFamily, VrpSolutionFamily>(CLUSTER_VRP);
      problem.setInput(FormattedValue.of(

          resourceProvider.readStream(problemStream), VRPFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(CLUSTER_VRP, e);
    }
  }
}
