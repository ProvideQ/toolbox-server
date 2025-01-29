package edu.kit.provideq.toolbox.circuit.processing.solver.optimization;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OptimizationSolver implements ProblemSolver<String, String> {

  @Override
  public String getName() {
    return "Optimize OpenQASM circuit";
  }

  @Override
  public String getDescription() {
    return "Run optimization algorithms on an OpenQASM circuit";
  }

  @Override
  public Mono<Solution<String>> solve(String input, SubRoutineResolver subRoutineResolver, SolvingProperties properties) {
    var solution = new Solution<>(this);
    solution.setSolutionData(input);
    return Mono.just(solution);
  }

  @Override
  public ProblemType<String, String> getProblemType() {
    return OptimizationConfiguration.OPTIMIZATION_CONFIG;
  }
}
