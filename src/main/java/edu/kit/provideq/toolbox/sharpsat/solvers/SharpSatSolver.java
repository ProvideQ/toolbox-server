package edu.kit.provideq.toolbox.sharpsat.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sat.SatFamily;
import edu.kit.provideq.toolbox.sharpsat.SharpSatConfiguration;
import edu.kit.provideq.toolbox.sharpsat.SolutionCountFamily;

public abstract class SharpSatSolver implements ProblemSolver<SatFamily, SolutionCountFamily> {
  @Override
  public ProblemType<SatFamily, SolutionCountFamily> getProblemType() {
    return SharpSatConfiguration.SHARPSAT;
  }
}
