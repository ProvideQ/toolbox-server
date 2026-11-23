package edu.kit.provideq.toolbox.solvers.tsp.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.tsp.TspConfiguration;
import edu.kit.provideq.toolbox.solvers.tsp.TspFamily;
import edu.kit.provideq.toolbox.solvers.tsp.TspSolutionFamily;

public abstract class TspSolver implements ProblemSolver<TspFamily, TspSolutionFamily> {
  @Override
  public ProblemType<TspFamily, TspSolutionFamily> getProblemType() {
    return TspConfiguration.TSP;
  }
}
