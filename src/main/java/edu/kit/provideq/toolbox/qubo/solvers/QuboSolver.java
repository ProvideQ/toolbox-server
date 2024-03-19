package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

/**
 * A solver for quadratic unconstrained binary optimization problems.
 */
public abstract class QuboSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType getProblemType() {
    return ProblemType.QUBO;
  }
}
