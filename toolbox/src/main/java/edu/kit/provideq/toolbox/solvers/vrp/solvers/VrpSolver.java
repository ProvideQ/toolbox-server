package edu.kit.provideq.toolbox.solvers.vrp.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.vrp.VrpConfiguration;
import edu.kit.provideq.toolbox.solvers.vrp.VrpFamily;
import edu.kit.provideq.toolbox.solvers.vrp.VrpSolutionFamily;

/**
 * A solver for VRP problems.
 */
public abstract class VrpSolver implements ProblemSolver<VrpFamily, VrpSolutionFamily> {
  @Override
  public ProblemType<VrpFamily, VrpSolutionFamily> getProblemType() {
    return VrpConfiguration.VRP;
  }
}
