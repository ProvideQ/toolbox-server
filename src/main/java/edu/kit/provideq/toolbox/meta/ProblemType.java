package edu.kit.provideq.toolbox.meta;

/**
 * The type of problem to solve.
 */
public enum ProblemType {
  /**
   * A satisfiability problem:
   * For a given boolean formula, check if there is an interpretation that satisfies the formula.
   */
  SAT,

  /**
   * An optimization problem:
   * For a given graph, find the optimal separation of vertices that maximises the cut crossing edge weight sum.
   */
  MAX_CUT
}
