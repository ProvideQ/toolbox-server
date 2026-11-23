package edu.kit.provideq.toolbox.solvers.sharpsat.solvers;

import edu.kit.provideq.formats.sat.SatFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.sharpsat.SharpSatConfiguration;
import edu.kit.provideq.toolbox.solvers.sharpsat.formats.SolutionCountFamily;

public abstract class SharpSatSolver implements ProblemSolver<SatFamily, SolutionCountFamily> {
  @Override
  public ProblemType<SatFamily, SolutionCountFamily> getProblemType() {
    return SharpSatConfiguration.SHARPSAT;
  }
}
