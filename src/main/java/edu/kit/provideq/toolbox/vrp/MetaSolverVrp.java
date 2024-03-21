package edu.kit.provideq.toolbox.vrp;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.vrp.solvers.ClusterAndSolveVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.LkhVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.QuboTspSolver;
import edu.kit.provideq.toolbox.vrp.solvers.VrpSolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for VRP problems.
 */
@Component
public class MetaSolverVrp extends MetaSolver<String, String, VrpSolver> {
  private final ResourceProvider resourceProvider;

  @Autowired
  public MetaSolverVrp(
          ResourceProvider resourceProvider,
          ClusterAndSolveVrpSolver clusterAndSolveVrpSolver,
          LkhVrpSolver lkhVrpSolver,
          QuboTspSolver quboTspSolver) {
    super(ProblemType.VRP, clusterAndSolveVrpSolver, lkhVrpSolver, quboTspSolver);
    this.resourceProvider = resourceProvider;
  }

  @Override
  public VrpSolver findSolver(Problem<String> problem, List<MetaSolverSetting> metaSolverSettings) {
    return (new ArrayList<>(this.solvers)).get(0);
  }

  @Override
  public List<String> getExampleProblems() {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("CMT1.vrp"),
          "Simple VRP CMT1 Problem unavailable!"
      );
      var problemTwoStream = Objects.requireNonNull(
          getClass().getResourceAsStream("SmallSample.vrp"),
          "Simple VRP SmallSample Problem unavailable!"
      );
      return List.of(resourceProvider.readStream(problemStream), resourceProvider.readStream(problemTwoStream));
    } catch (IOException e) {
      throw new MissingExampleException("Could not load example problems", e);
    }
  }
}
