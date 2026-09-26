package edu.kit.provideq.toolbox.meta.strategy;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.SolverCharacteristics;
import java.util.ArrayList;
import java.util.List;

/**
 * A single transformation rule of a meta-solver strategy, a problem and the solver applied to it
 * A step without a problem id stands for a sub-routine that hasn't been called yet
 */
public record StrategyStep(
    String problemId,
    String problemTypeId,
    int depth,
    String solverName,
    SolverCharacteristics characteristics
) {
  /**
   * Flattens strategy formed by a problem and its sub-problems into pre-order
   * so every step directly follows its parent
   */
  public static List<StrategyStep> collect(Problem<?, ?> problem) {
    var steps = new ArrayList<StrategyStep>();
    collect(problem, 0, steps);
    return steps;
  }

  private static void collect(Problem<?, ?> problem, int depth, List<StrategyStep> steps) {
    var solver = problem.getSolver().orElse(null);

    steps.add(new StrategyStep(
        problem.getId().toString(),
        problem.getType().getId(),
        depth,
        solver == null ? null : solver.getName(),
        solver == null ? null : solver.getCharacteristics()));

    if (solver == null) {
      return;
    }

    for (var subRoutine : solver.getSubRoutines()) {
      var subProblems = problem.getSubProblems(subRoutine);
      if (subProblems.isEmpty()) {
        steps.add(new StrategyStep(null, subRoutine.type().getId(), depth + 1, null, null));
      }
      for (var subProblem : subProblems) {
        collect(subProblem, depth + 1, steps);
      }
    }
  }

  public boolean isConfigured() {
    return characteristics != null;
  }
}
