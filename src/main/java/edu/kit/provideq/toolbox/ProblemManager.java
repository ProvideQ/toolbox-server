package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.TypedProblemType;
import edu.kit.provideq.toolbox.test.Problem;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Manages all solutions currently present in memory.
 */
public class ProblemManager<InputT, ResultT> {
  private final TypedProblemType<InputT, ResultT> problemType;
  private final Set<ProblemSolver<InputT, ResultT>> problemSolvers;
  private final Set<Problem<InputT, ResultT>> problems;
  private final Set<Problem<InputT, ResultT>> exampleProblems;

  public ProblemManager(
      TypedProblemType<InputT, ResultT> type,
      Collection<ProblemSolver<InputT, ResultT>> solvers,
      Collection<Problem<InputT, ResultT>> exampleProblems
  ) {
    this.problemType = type;
    this.problems = new HashSet<>();
    this.problemSolvers = new HashSet<>(solvers);
    this.exampleProblems = new HashSet<>(exampleProblems);

    if (this.exampleProblems.isEmpty()) {
      throw new IllegalArgumentException("Example problems may not be empty!");
    }
  }

  /**
   * Registers a problem within this problem manager.
   *
   * @param problem the problem to register.
   */
  public void addProblem(Problem<InputT, ResultT> problem) {
    this.problems.add(problem);
  }

  /**
   * Finds a problem of this manager's type with a given {@code id}.
   *
   * @param id the ID of the problem to find.
   * @return an optional containing the problem with the given {@code id} or an empty optional otherwise.
   */
  public Optional<Problem<InputT, ResultT>> getProblemById(UUID id) {
    return this.problems.stream()
        .filter(problem -> problem.getId().equals(id))
        .findFirst();
  }

  public TypedProblemType<InputT, ResultT> getProblemType() {
    return problemType;
  }

  public Set<ProblemSolver<InputT, ResultT>> getProblemSolvers() {
    return Collections.unmodifiableSet(this.problemSolvers);
  }

  public Optional<ProblemSolver<InputT, ResultT>> getProblemSolverById(String id) {
    return this.getProblemSolvers().stream()
        .filter(solver -> solver.getId().equals(id))
        .findAny();
  }

  public Set<Problem<InputT, ResultT>> getExampleProblems() {
    return Collections.unmodifiableSet(this.exampleProblems);
  }

  public Set<Problem<InputT, ResultT>> getProblems() {
    return Collections.unmodifiableSet(this.problems);
  }

  public void removeProblem(Problem<InputT, ResultT> problem) {
    this.problems.remove(problem);
  }
}
