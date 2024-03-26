package edu.kit.provideq.toolbox.meta;

/**
 * Describes the state of the solution process of a {@link Problem}.
 */
public enum ProblemState {
  /**
   * The problem is missing some required configuration (e.g., input or solver) before the solution
   * process can be started.
   */
  NEEDS_CONFIGURATION,

  /**
   * The problem is configured and can be solved using the {@link #solve()} method.
   */
  READY_TO_SOLVE,

  /**
   * A {@link ProblemSolver} is currently computing the solution of this problem.
   */
  SOLVING,

  /**
   * The problem has been solved and the solution can be accessed via {@link Problem#getSolution()}.
   */
  SOLVED
}
