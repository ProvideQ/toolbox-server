package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sat.SatConfiguration;

/**
 * A solver for SAT problems.
 */
public abstract class SatSolver implements ProblemSolver<String, DimacsCnfSolution> {
  @Override
  public ProblemType<String, DimacsCnfSolution> getProblemType() {
    return SatConfiguration.SAT;
  }
}
