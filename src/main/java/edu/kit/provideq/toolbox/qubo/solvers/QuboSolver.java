package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import edu.kit.provideq.toolbox.qubo.QuboFamily;
import edu.kit.provideq.toolbox.qubo.QuboSolutionFamily;

/**
 * A solver for quadratic unconstrained binary optimization problems.
 */
public abstract class QuboSolver implements ProblemSolver<QuboFamily, QuboSolutionFamily> {
  @Override
  public ProblemType<QuboFamily, QuboSolutionFamily> getProblemType() {
    return QuboConfiguration.QUBO;
  }
}
