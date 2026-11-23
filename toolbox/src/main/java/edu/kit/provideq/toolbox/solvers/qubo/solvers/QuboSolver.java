package edu.kit.provideq.toolbox.solvers.qubo.solvers;

import edu.kit.provideq.formats.lp.QuboFamily;
import edu.kit.provideq.formats.lp.QuboSolutionFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.qubo.QuboConfiguration;

/**
 * A solver for quadratic unconstrained binary optimization problems.
 */
public abstract class QuboSolver implements ProblemSolver<QuboFamily, QuboSolutionFamily> {
  @Override
  public ProblemType<QuboFamily, QuboSolutionFamily> getProblemType() {
    return QuboConfiguration.QUBO;
  }
}
