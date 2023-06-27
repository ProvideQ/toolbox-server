package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;

import java.util.Collections;
import java.util.List;

/**
 * A problem solver provides information about its own suitability to solve a given problem.
 * It can solve problems and write the resulting data in a provided {@link Solution} object.
 */
public interface ProblemSolver<ProblemFormatType, SolutionDataType> {
  /**
   * returns an id which is unique to the solver
   *
   * @return id of the solver
   */
  default String getId() {
    return getClass().getName();
  }

  /**
   * returns the name of the solver
   *
   * @return name of the solver
   */
  String getName();

  /**
   * returns the sub problems used to solver this problem
   *
   * @return list of sub problems
   */
  default List<SubRoutineDefinition> getSubRoutines() {
    return Collections.emptyList();
  }

  /**
   * simple true-false-check, true: given {@link Problem} can be solved with this solver, false: it cannot
   *
   * @param problem the {@link Problem} which is to be assessed
   * @return true: can be solved, false: can not be solved
   */
  boolean canSolve(Problem<ProblemFormatType> problem);

  /**
   * suitability self assessment, results may range from 0.0 to 1.0, 1.0 meaning perfect suitability
   *
   * @param problem the {@link Problem} which is to be assessed
   * @return suitability ranging from 0.0 to 1.0
   */
  float getSuitability(Problem<ProblemFormatType> problem);

  /**
   * solves a given {@link Problem}, current status and final results as well as debug information is
   * stored in the provided {@link Solution} object
   *
   * @param problem        the {@link Problem} that is to be solved
   * @param solution       the {@link Solution} in which all resulting information is to be stored
   * @param subRoutinePool {@link SubRoutinePool} pool to retrieve sub routine from
   */
  void solve(Problem<ProblemFormatType> problem, Solution<SolutionDataType> solution,
             SubRoutinePool subRoutinePool);
}
