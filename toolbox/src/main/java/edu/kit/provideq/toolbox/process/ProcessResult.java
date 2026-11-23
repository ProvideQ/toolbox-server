package edu.kit.provideq.toolbox.process;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.meta.Solution;
import java.util.Optional;
import java.util.function.Function;

/**
 * Result of running a process.
 *
 * @param success did the process complete successfully
 * @param output  process console output
 */
public record ProcessResult<T>(boolean success, Optional<T> output, Optional<String> errorOutput) {
  /**
   * Utility method for storing the contents of a process result in a solution object.
   * The output is parsed into the given format's representation.
   *
   * @param solution the solution to apply this data to.
   * @param format   the format the process output is expressed in
   * @param <SolOutT>   output format family of the solution
   * @param <SolT>      representation type of the given solution format
   * @return the given, modified solution.
   */
  @SuppressWarnings("unchecked")
  public <SolOutT extends FormatFamily, SolT> Solution<SolOutT> applyTo(
      Solution<SolOutT> solution,
      Format<SolT> format,
      Function<T, SolT> mapper) {
    if (this.success) {
      if (output().isPresent()) {
        var output = output().get();
        if (output instanceof String text) {
          try {
            solution.setSolutionData(format.parse(text), format);
          } catch (ParseException e) {
            solution.setDebugData("Could not parse process output as " + format.getName()
                + ": " + e.getMessage());
            solution.fail();
            return solution;
          }
        } else {
          solution.setSolutionData(mapper.apply(output), format);
        }
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