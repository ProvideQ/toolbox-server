package edu.kit.provideq.toolbox.knapsack.solvers;

import edu.kit.provideq.toolbox.knapsack.KnapsackConfiguration;
import edu.kit.provideq.toolbox.knapsack.KnapsackFamily;
import edu.kit.provideq.toolbox.knapsack.KnapsackSolutionFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

/**
 * A solver for Knapsack problems.
 */
public abstract class KnapsackSolver
    implements ProblemSolver<KnapsackFamily, KnapsackSolutionFamily> {
  @Override
  public ProblemType<KnapsackFamily, KnapsackSolutionFamily> getProblemType() {
    return KnapsackConfiguration.KNAPSACK;
  }
}
