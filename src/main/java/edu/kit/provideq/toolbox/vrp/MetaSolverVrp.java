package edu.kit.provideq.toolbox.vrp;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.vrp.solvers.ClusterAndSolveVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.LkhVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.TestVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.VrpSolver;

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
public class MetaSolverVrp extends MetaSolver<String, String, VrpSolver> {
  private final String examplesDirectoryPath;
  private final ResourceProvider resourceProvider;

  @Autowired
  public MetaSolverVrp(
          @Value("${examples.directory.vrp}") String examplesDirectoryPath,
          ResourceProvider resourceProvider,
          TestVrpSolver testVrpSolver,
          ClusterAndSolveVrpSolver clusterAndSolveVrpSolver,
          LkhVrpSolver lkhVrpSolver) {
    super(ProblemType.VRP, testVrpSolver, clusterAndSolveVrpSolver, lkhVrpSolver);
    this.examplesDirectoryPath = examplesDirectoryPath;
    this.resourceProvider = resourceProvider;
  }

  @Override
  public VrpSolver findSolver(Problem<String> problem, List<MetaSolverSetting> metaSolverSettings) {
    // todo add decision
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }

  @Override
  public List<String> getExampleProblems() {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("CMT1.vrp"),
          "Simple VRP CMT1 Problem unavailable!"
      );
      return List.of(resourceProvider.readStream(problemStream));
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
