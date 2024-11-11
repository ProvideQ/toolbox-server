package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.maxcut.MaxCutConfiguration;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

/**
 * A solver for MaxCut problems.
 */
public abstract class MaxCutSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType<String, String> getProblemType() {
    return MaxCutConfiguration.MAX_CUT;
  }
}
