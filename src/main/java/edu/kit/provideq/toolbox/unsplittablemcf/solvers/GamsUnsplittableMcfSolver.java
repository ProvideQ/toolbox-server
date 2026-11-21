package edu.kit.provideq.toolbox.unsplittablemcf.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.unsplittablemcf.UmcfFamily;
import edu.kit.provideq.toolbox.unsplittablemcf.UmcfSolutionFamily;
import edu.kit.provideq.toolbox.unsplittablemcf.UnsplittableMcfConfiguration;

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
