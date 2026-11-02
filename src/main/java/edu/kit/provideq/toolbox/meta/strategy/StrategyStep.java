package edu.kit.provideq.toolbox.meta.strategy;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.SolverCharacteristics;
import edu.kit.provideq.toolbox.meta.guard.Guard;
import edu.kit.provideq.toolbox.meta.setting.basic.BooleanSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record StrategyStep(
    String problemId,
    String problemTypeId,
    int depth,
    String solverName,
    SolverCharacteristics characteristics,
    boolean guarded,
    String guardedBy
) {
  /**
   * Flattens strategy formed by a problem and its sub-problems into pre-order
   * so every step directly follows its parent.
   */
  public static List<StrategyStep> collect(Problem<?, ?> problem) {
    var steps = new ArrayList<StrategyStep>();
    collect(problem, 0, null, steps);
    return steps;
  }

  private static void collect(
      Problem<?, ?> problem,
      int depth,
      String guardedBy,
      List<StrategyStep> steps
  ) {
    var solver = problem.getSolver().orElse(null);
    var problemId = problem.getId().toString();
    var guarded = solver != null && isGuardEnabled(problem, solver);
    if (guarded && guardedBy == null) {
      guardedBy = problemId;
    }

    steps.add(new StrategyStep(
        problemId,
        problem.getType().getId(),
        depth,
        solver == null ? null : solver.getName(),
        solver == null ? null : solver.getCharacteristics(),
        guarded,
        guardedBy));

    if (solver == null) {
      return;
    }

    for (var subRoutine : solver.getSubRoutines()) {
      var subProblems = problem.getSubProblems(subRoutine);
      if (subProblems.isEmpty()) {
        steps.add(new StrategyStep(
            null, subRoutine.type().getId(), depth + 1, null, null, false, guardedBy));
      }
      for (var subProblem : subProblems) {
        collect(subProblem, depth + 1, guardedBy, steps);
      }
    }
  }

  private static boolean isGuardEnabled(Problem<?, ?> problem, ProblemSolver<?, ?> solver) {
    return Stream.concat(
            problem.getSolverSettings().stream(),
            solver.getSolverSettings().stream())
        .filter(setting -> setting.getName().equals(Guard.SETTING_NAME))
        .filter(BooleanSetting.class::isInstance)
        .map(setting -> ((BooleanSetting) setting).getState())
        .findFirst()
        .orElse(false);
  }

  public boolean isConfigured() {
    return characteristics != null;
  }
}
