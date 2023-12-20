package edu.kit.provideq.toolbox;

/**
 * Result of running a process.
 *
 * @param success  did the process complete successfully
 * @param solution the solution to the problem
 * @param output   process console output
 */
public record RunnerResult<SolutionT>(boolean success, SolutionT solution, String output) {

}
