package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;

import static edu.kit.provideq.toolbox.SolutionStatus.ERROR;
import static edu.kit.provideq.toolbox.SolutionStatus.INVALID;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#SAT} solver using a GAMS implementation.
 */
@Component
public class ClusterAndSolveVrpSolver extends VrpSolver {
  private final ApplicationContext context;

  @Autowired
  public ClusterAndSolveVrpSolver(
      ApplicationContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return "Clustering VRP Solver";
  }

  @Override
  public List<SubRoutineDefinition> getSubRoutines() {
    return List.of(
        new SubRoutineDefinition(ProblemType.CLUSTERABLE_VRP,
            "How should the VRP be clustered?"));
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.VRP;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {
    var vrpClusterer = subRoutinePool.<String, String>getSubRoutine(ProblemType.CLUSTERABLE_VRP);

    var vrpSolution = vrpClusterer.apply(problem.problemData());


    if (vrpSolution.getStatus() == INVALID) {
      solution.setDebugData(vrpSolution.getDebugData());
      solution.abort();
      return;
    }

    solution.setSolutionData(vrpSolution.getSolutionData());
    solution.complete();
  }
}
