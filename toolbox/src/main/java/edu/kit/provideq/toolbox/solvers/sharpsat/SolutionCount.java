package edu.kit.provideq.toolbox.solvers.sharpsat;

/**
 * Object representation of a #SAT solution: the number of satisfying assignments.
 *
 * @param count the number of interpretations that satisfy the formula
 */
public record SolutionCount(long count) {
}
