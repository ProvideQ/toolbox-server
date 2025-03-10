package edu.kit.provideq.toolbox.mip.solvers;

import edu.kit.provideq.toolbox.mip.MipConfiguration;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

public abstract class MipSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType<String, String> getProblemType() {
    return MipConfiguration.MIP;
  }
}
