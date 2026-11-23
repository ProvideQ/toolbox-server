package edu.kit.provideq.toolbox.solvers.unsplittablemcf.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.unsplittablemcf.UmcfFamily;
import edu.kit.provideq.toolbox.solvers.unsplittablemcf.UmcfSolutionFamily;
import edu.kit.provideq.toolbox.solvers.unsplittablemcf.UnsplittableMcfConfiguration;

/**
 * Base class for Unsplittable Multi Commodity Flow solvers.
 */
public abstract class GamsUnsplittableMcfSolver
    implements ProblemSolver<UmcfFamily, UmcfSolutionFamily> {
  @Override
  public ProblemType<UmcfFamily, UmcfSolutionFamily> getProblemType() {
    return UnsplittableMcfConfiguration.UNSPLITTABLE_MCF;
  }
}
