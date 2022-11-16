package edu.kit.provideq.toolbox.meta;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MetaSolver<T extends ProblemSolver> {

  Set<T> solvers = new HashSet<>();

  public MetaSolver() {
  }

  public MetaSolver(List<T> problemSolvers) {
    solvers.addAll(problemSolvers);
  }

  public MetaSolver(T... problemSolvers) {
    solvers.addAll(List.of(problemSolvers));
  }

  public boolean registerSolver(T problemSolver) {
    return solvers.add(problemSolver);
  }

  public boolean unregisterSolver(T problemSolver) {
    return solvers.remove(problemSolver);
  }

  public T findSolver(Problem problem) {
    Optional<T> solver = solvers.stream()
        .filter(s -> s.canSolve(problem))
        .max(Comparator.comparing(s -> s.getSuitability(problem)));
    return solver.orElse(null);
  }

}
