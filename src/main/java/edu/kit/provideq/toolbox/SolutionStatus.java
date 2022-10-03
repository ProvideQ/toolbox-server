package edu.kit.provideq.toolbox;

/**
 * Describes the state of a solution process.
 */
public enum SolutionStatus {
  /**
   * The problem cannot be solved because the input is invalid.
   */
  INVALID,
  /**
   * The solution is currently being computed.
   */
  COMPUTING,
  /**
   * The problem has been solved and the solution is attached.
   */
  SOLVED;
}
