package edu.kit.provideq.toolbox.tsp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import reactor.core.publisher.Mono;

public class LkhTspSolver extends TspSolver {

  @Override
  public String getName() {
    return "";
  }

  @Override
  public Mono<Solution<String>> solve(String input, SubRoutineResolver subRoutineResolver) {
    return null;
  }
}
