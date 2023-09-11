package edu.kit.provideq.toolbox.meta;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Stream;

public class MetaSolverHelper {
  public static Stream<List<Object>> getAllArgumentCombinations(MetaSolver<?, ?, ?> metaSolver) {
    // Convert all solvers to their solver id
    var solvers = metaSolver.getAllSolvers().stream()
            .map(x -> x.getClass().getName())
            .toList();

    // Get all example problems
    var problems = metaSolver.getExampleProblems();

    // Return all combinations
    return Lists.cartesianProduct(solvers, problems).stream();
  }
}
