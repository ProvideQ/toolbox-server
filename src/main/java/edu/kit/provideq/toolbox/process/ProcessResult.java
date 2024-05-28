package edu.kit.provideq.toolbox.process;

import edu.kit.provideq.toolbox.Solution;

import java.util.Optional;

/**
 * Result of running a process.
 *
 * @param success did the process complete successfully
 * @param output  process console output
 */
public record ProcessResult<T>(boolean success, Optional<T> output, Optional<String> errorOutput) {
  /**
   * Utility method for storing the contents of a process result in a string solution object.
   *
   * @param solution the solution to apply this data to.
   * @return the given, modified solution.
   */
  public Solution<T> applyTo(Solution<T> solution) {
    if (this.success) {
        if(output().isPresent()) {
            solution.setSolutionData(output().get());
        } else {
            solution.setDebugData("Solution was found, but could not retrieve Solution Data");
        }
        solution.complete();
    } else {
        solution.setDebugData(errorOutput().orElse("Unknown error occurred."));
        solution.fail();
    }
    return solution;
  }
}
