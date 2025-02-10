package edu.kit.provideq.toolbox.sharpsat.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sharpsat.SharpSatConfiguration;

public abstract class SharpSatSolver implements ProblemSolver<String, Integer> {
  @Override
  public ProblemType<String, Integer> getProblemType() {
    return SharpSatConfiguration.SHARPSAT;
  }
}
