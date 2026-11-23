package edu.kit.provideq.toolbox.solvers.tsp.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.tsp.TspConfiguration;
import edu.kit.provideq.formats.tsplib.TspLibFamily;
import edu.kit.provideq.formats.tsplib.TspSolutionFamily;

public abstract class TspSolver implements ProblemSolver<TspLibFamily, TspSolutionFamily> {
  @Override
  public ProblemType<TspLibFamily, TspSolutionFamily> getProblemType() {
    return TspConfiguration.TSP;
  }
}
