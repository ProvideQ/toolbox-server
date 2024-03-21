package edu.kit.provideq.toolbox.vrp.clusterer;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.vrp.MetaSolverVrp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for VRP problems.
 */
@Component
public class MetaSolverClusterVrp extends MetaSolver<String, String, VrpClusterer> {
  private final String examplesDirectoryPath;
  private final ResourceProvider resourceProvider;

  @Autowired
  public MetaSolverClusterVrp(
          @Value("${examples.directory.vrp}") String examplesDirectoryPath,
          ResourceProvider resourceProvider,
          NoClusteringClusterer noClusteringClusterer,
          TwoPhaseClusterer twoPhaseClusterer,
          KmeansClusterer kmeansClusterer) {
    super(ProblemType.CLUSTERABLE_VRP, noClusteringClusterer, twoPhaseClusterer, kmeansClusterer);
    this.examplesDirectoryPath = examplesDirectoryPath;
    this.resourceProvider = resourceProvider;
  }

  @Override
  public VrpClusterer findSolver(Problem<String> problem, List<MetaSolverSetting> metaSolverSettings) {
    return (new ArrayList<>(this.solvers)).get(0);
  }

  @Override
  public List<String> getExampleProblems() {
    try {
      var problemStream = Objects.requireNonNull(
          MetaSolverVrp.class.getResourceAsStream("CMT1.vrp"),
          "Simple VRP CMT1 Problem unavailable!"
      );
      return List.of(resourceProvider.readStream(problemStream));
    } catch (IOException e) {
      throw new MissingExampleException("Could not load example problems", e);
    }
  }
}
