package edu.kit.provideq.toolbox;

/**
 * Describes the state of a solution process.
 */
public enum SolutionStatus {

  /**
   * The problem cannot be solved because the input is invalid.
   */
  INVALID(true),

  /**
   * The solution is currently being computed.
   */
  COMPUTING(false),

  /**
   * The problem has been solved and the solution is attached.
   */
  SOLVED(true),

  /**
   * The problem could not be solved.
   */
  ERROR(true);

  /**
   * The solution is currently being computed.
   */
  private final boolean isCompleted;

  private SolutionStatus(boolean isCompleted) {
    this.isCompleted = isCompleted;
  }

  public boolean isCompleted() {
    return this.isCompleted;
  }
}
