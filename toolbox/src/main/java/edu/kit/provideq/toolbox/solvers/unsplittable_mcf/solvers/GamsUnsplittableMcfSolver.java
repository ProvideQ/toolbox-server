package edu.kit.provideq.toolbox.solvers.unsplittable_mcf.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.formats.UmcfFamily;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.formats.UmcfSolutionFamily;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.UnsplittableMcfConfiguration;

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
