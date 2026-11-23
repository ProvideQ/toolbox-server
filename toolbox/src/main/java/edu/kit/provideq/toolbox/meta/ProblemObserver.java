package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.families.FormatFamily;

/**
 * A problem observer is notified when a problem changes.
 */
public interface ProblemObserver<InT extends FormatFamily, OutT extends FormatFamily> {
  /**
   * Called when the input of an observed problem changes.
   *
   * @param problem the problem whose input changed.
   * @param newInput the new input of the problem.
   */
  void onInputChanged(Problem<InT, OutT> problem, FormattedValue<?> newInput);

  /**
   * Called when the solver of an observed problem changes.
   *
   * @param problem the problem whose solver changed.
   * @param newSolver the new solver of the problem.
   */
  void onSolverChanged(Problem<InT, OutT> problem, ProblemSolver<InT, OutT> newSolver);

  /**
   * Called when the solver of an observed problem is reset to null.
   *
   * @param problem the problem whose solver changed to null.
   */
  void onSolverReset(Problem<InT, OutT> problem);

  /**
   * Called when the state of an observed problem changes.
   *
   * @param problem the problem whose state changed.
   * @param newState the new state of the problem.
   */
  void onStateChanged(Problem<InT, OutT> problem, ProblemState newState);

  /**
   * Called when a sub-problem is added to a problem.
   *
   * @param problem the problem that a new sub-problem was added to.
   * @param addedSubProblem the sub-problem that was added to the problem.
   */
  <SubInT extends FormatFamily, SubOutT extends FormatFamily> void onSubProblemAdded(
      Problem<InT, OutT> problem,
      Problem<SubInT, SubOutT> addedSubProblem
  );

  /**
   * Called when a sub-problem is removed from a problem.
   *
   * @param problem the problem that a sub-problem was removed from.
   * @param removedSubProblem the sub-problem that was removed from the problem.
   */
  <SubInT extends FormatFamily, SubOutT extends FormatFamily> void onSubProblemRemoved(
      Problem<InT, OutT> problem,
      Problem<SubInT, SubOutT> removedSubProblem
  );
}