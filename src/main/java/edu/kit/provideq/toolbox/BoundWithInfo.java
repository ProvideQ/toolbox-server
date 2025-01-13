package edu.kit.provideq.toolbox;

/**
 * Represents a bound estimation for the solution of a problem.
 *
 * @param bound the value
 * @param executionTime the time it took to estimate the value
 */
public record BoundWithInfo(Bound bound, long executionTime) {
}
