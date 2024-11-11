package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.vrp.VrpConfiguration;

/**
 * A solver for VRP problems.
 */
public abstract class VrpSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType<String, String> getProblemType() {
    return VrpConfiguration.VRP;
  }
}
