package edu.kit.provideq.toolbox.knapsack.solvers;

import edu.kit.provideq.toolbox.knapsack.KnapsackConfiguration;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;

/**
 * A solver for Knapsack problems.
 */
public abstract class KnapsackSolver implements ProblemSolver<String, String> {
    @Override
    public ProblemType<String, String> getProblemType() {
        return KnapsackConfiguration.KNAPSACK;
    }
}
