package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.maxcut.MaxCutConfiguration;
import edu.kit.provideq.toolbox.maxcut.MaxCutFamily;
import edu.kit.provideq.toolbox.maxcut.MaxCutSolutionFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

/**
 * A solver for MaxCut problems.
 */
public abstract class MaxCutSolver implements ProblemSolver<MaxCutFamily, MaxCutSolutionFamily> {
  @Override
  public ProblemType<MaxCutFamily, MaxCutSolutionFamily> getProblemType() {
    return MaxCutConfiguration.MAX_CUT;
  }
}
