package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.test.SubRoutineResolver;
import java.util.Collections;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * A problem solver provides information about its own suitability to solve a given problem.
 * It can solve problems and write the resulting data in a provided {@link Solution} object.
 */
public interface ProblemSolver<ProblemT, SolutionT> {
  /**
   * Returns an id which is unique to the solver.
   *
   * @return id of the solver
   */
  default String getId() {
    return getClass().getName();
  }

  /**
   * Returns the name of the solver.
   *
   * @return name of the solver
   */
  String getName();

  /**
   * Returns the sub problems used to solver this problem.
   *
   * @return list of sub problems
   */
  default List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return Collections.emptyList();
  }

  /**
   * Simple true-false-check,
   * {@code true}: given {@link Problem} can be solved with this solver,
   * {@code false}: it cannot.
   *
   * @param problem the {@link Problem} which is to be assessed
   * @return true: can be solved, false: can not be solved
   */
  boolean canSolve(Problem<ProblemT> problem);

  /**
   * Solves a given {@link Problem}, current status and final results as well as debug information
   * is stored in the provided {@link Solution} object.
   *
   * @param problem the problem that is to be solved.
   * @param subRoutineResolver used to call sub-routines declared in {@link #getSubRoutines()}.
   */
  Mono<Solution<SolutionT>> solve(Problem<ProblemT> problem,
                                  SubRoutineResolver subRoutineResolver);
}
