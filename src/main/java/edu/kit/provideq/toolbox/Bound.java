package edu.kit.provideq.toolbox;

/**
 * Represents a bound estimation for the solution of a problem.
 *
 * @param bound the bound value
 * @param boundType the type of the bound
 * @param executionTime the time it took to estimate the bound
 */
public record Bound(String bound, BoundType boundType, long executionTime) {
}
