package edu.kit.provideq.toolbox.tsp.solvers;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.tsp.TspConfiguration;

public abstract class TspSolver implements ProblemSolver<String, String> {
  @Override
  public ProblemType<String, String> getProblemType() {
    return TspConfiguration.TSP;
  }
}
