package edu.kit.provideq.toolbox.meta;

/**
 * A problem observer is notified when a problem changes.
 */
public interface ProblemObserver<InputT, ResultT> {
  /**
   * Called when the input of an observed problem changes.
   *
   * @param problem the problem whose input changed.
   * @param newInput the new input of the problem.
   */
  void onInputChanged(Problem<InputT, ResultT> problem, InputT newInput);

  /**
   * Called when the solver of an observed problem changes.
   *
   * @param problem the problem whose solver changed.
   * @param newSolver the new solver of the problem.
   */
  void onSolverChanged(Problem<InputT, ResultT> problem, ProblemSolver<InputT, ResultT> newSolver);

  /**
   * Called when the solver of an observed problem is reset to null.
   *
   * @param problem the problem whose solver changed to null.
   */
  void onSolverReset(Problem<InputT, ResultT> problem);

  /**
   * Called when the state of an observed problem changes.
   *
   * @param problem the problem whose state changed.
   * @param newState the new state of the problem.
   */
  void onStateChanged(Problem<InputT, ResultT> problem, ProblemState newState);

  /**
   * Called when a sub-problem is added to a problem.
   *
   * @param problem the problem that a new sub-problem was added to.
   * @param addedSubProblem the sub-problem that was added to the problem.
   */
  <SubInputT, SubResultT> void onSubProblemAdded(
      Problem<InputT, ResultT> problem,
      Problem<SubInputT, SubResultT> addedSubProblem
  );

  /**
   * Called when a sub-problem is removed from a problem.
   *
   * @param problem the problem that a sub-problem was removed from.
   * @param removedSubProblem the sub-problem that was removed from the problem.
   */
  <SubInputT, SubResultT> void onSubProblemRemoved(
      Problem<InputT, ResultT> problem,
      Problem<SubInputT, SubResultT> removedSubProblem
  );
}
