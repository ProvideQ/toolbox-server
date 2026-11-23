package edu.kit.provideq.toolbox.solvers.maxcut.solvers;

import edu.kit.provideq.formats.gml.MaxCutFamily;
import edu.kit.provideq.formats.gml.MaxCutSolutionFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.maxcut.MaxCutConfiguration;

/**
 * A solver for MaxCut problems.
 */
public abstract class MaxCutSolver implements ProblemSolver<MaxCutFamily, MaxCutSolutionFamily> {
  @Override
  public ProblemType<MaxCutFamily, MaxCutSolutionFamily> getProblemType() {
    return MaxCutConfiguration.MAX_CUT;
  }
}
