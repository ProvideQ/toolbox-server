package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
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
public abstract class MetaSolver<T extends ProblemSolver> {

  protected Set<T> solvers = new HashSet<>();

  public MetaSolver() {
  }

  public MetaSolver(List<T> problemSolvers) {
    solvers.addAll(problemSolvers);
  }

  public MetaSolver(T... problemSolvers) {
    solvers.addAll(List.of(problemSolvers));
  }

  /**
   * Adds a new solver to this meta solvers list of known solvers.
   *
   * @param problemSolver the new problem solver
   * @return true in case the addition was successful, false otherwise
   */
  public boolean registerSolver(T problemSolver) {
    return solvers.add(problemSolver);
  }

  /**
   * Removes a solver from this meta solvers list of known solvers.
   *
   * @param problemSolver the solver
   * @return true in case the removal was successful, false otherwise
   */
  public boolean unregisterSolver(T problemSolver) {
    return solvers.remove(problemSolver);
  }

  /**
   * Provides the best suited known solver this meta solver is aware of for a given problem.
   *
   * @param problem the problem the meta solver is to check its solvers by
   * @return the best suited solver, null in case no suitable solver was found
   */
  public abstract T findSolver(Problem problem, List<MetaSolverSetting> metaSolverSettings);

  public Optional<T> getSolver(String id) {
    if (id == null) {
      return Optional.empty();
    }

    return solvers.stream()
        .filter(solver -> solver.getId().equals(id))
        .findFirst();
  }

  /**
   * Provides a list of all solvers registered on this meta solver.
   *
   * @return list of all solvers
   */
  public Set<T> getAllSolvers() {
    return new HashSet<>(solvers);
  }

  public List<MetaSolverSetting> getSettings() {
    return List.of();
  }

  @Override
  public int hashCode() {
    return this.solvers.hashCode();
  }
}
