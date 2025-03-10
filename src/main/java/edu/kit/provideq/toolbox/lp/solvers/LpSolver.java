package edu.kit.provideq.toolbox.lp.solvers;

import edu.kit.provideq.toolbox.lp.LpConfiguration;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

public abstract class LpSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType<String, String> getProblemType() {
    return LpConfiguration.LP;
  }
}
