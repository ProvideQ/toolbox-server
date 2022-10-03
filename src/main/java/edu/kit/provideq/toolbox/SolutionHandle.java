package edu.kit.provideq.toolbox;

/**
 * Used to refer to the solution (process) of a submitted problem.
 * @param id the unique identifier for this solution (process).
 * @param status the current status of the solution process.
 */
public record SolutionHandle(long id, SolutionStatus status) {
}
