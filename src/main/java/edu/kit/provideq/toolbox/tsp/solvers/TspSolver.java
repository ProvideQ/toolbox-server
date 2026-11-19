package edu.kit.provideq.toolbox.tsp.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.tsp.TspConfiguration;
import edu.kit.provideq.toolbox.tsp.TspFamily;
import edu.kit.provideq.toolbox.tsp.TspSolutionFamily;

public abstract class TspSolver implements ProblemSolver<TspFamily, TspSolutionFamily> {
  @Override
  public ProblemType<TspFamily, TspSolutionFamily> getProblemType() {
    return TspConfiguration.TSP;
  }
}
