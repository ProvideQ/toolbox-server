package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sat.SatConfiguration;
import edu.kit.provideq.toolbox.sat.SatFamily;
import edu.kit.provideq.toolbox.sat.SatSolutionFamily;

/**
 * A solver for SAT problems.
 */
public abstract class SatSolver implements ProblemSolver<SatFamily, SatSolutionFamily> {
  @Override
  public ProblemType<SatFamily, SatSolutionFamily> getProblemType() {
    return SatConfiguration.SAT;
  }
}
