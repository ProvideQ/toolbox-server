package edu.kit.provideq.toolbox.meta;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which known {@link ProblemSolver} is suited best for a given problem,
 * manages known solvers.
 *
 * @param <T> the type of {@link ProblemSolver} this metasolver is to manage
 */
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

  /**
   * adds a new solver to this meta solvers list of known solvers
   * @param problemSolver the new problem solver
   * @return true in case the addition was successful, false otherwise
   */
  public boolean registerSolver(T problemSolver) {
    return solvers.add(problemSolver);
  }

  /**
   * removes a solver from this meta solvers list of known solvers
   * @param problemSolver the solver
   * @return true in case the removal was successful, false otherwise
   */
  public boolean unregisterSolver(T problemSolver) {
    return solvers.remove(problemSolver);
  }

  /**
   * provides the best suited known solver this meta solver is aware of for a given problem
   * @param problem the problem the meta solver is to check its solvers by
   * @return the best suited solver, null in case no suitable solver was found
   */
  public T findSolver(Problem problem) {
    Optional<T> solver = solvers.stream()
        .filter(s -> s.canSolve(problem))
        .max(Comparator.comparing(s -> s.getSuitability(problem)));
    return solver.orElse(null);
  }

  @Override
  public int hashCode() {
    return this.solvers.hashCode();
  }
}
