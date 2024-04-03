package edu.kit.provideq.toolbox;

/**
 * Result of running a process.
 *
 * @param success did the process complete successfully
 * @param output  process console output
 */
public record ProcessResult(boolean success, String output) {
  /**
   * Utility method for storing the contents of a process result in a string solution object.
   *
   * @param solution the solution to apply this data to.
   * @return the given, modified solution.
   */
  public Solution<String> applyTo(Solution<String> solution) {
    if (this.success) {
      solution.setSolutionData(this.output);
      solution.complete();
    } else {
      solution.setDebugData(this.output);
      solution.fail();
    }
    return solution;
  }
}
