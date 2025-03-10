package edu.kit.provideq.toolbox.circuit.processing.solver.mitigation;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ErrorMitigationSolver implements ProblemSolver<String, String> {

  @Override
  public String getName() {
    return "Mitigate Errors for OpenQASM";
  }

  @Override
  public String getDescription() {
    return "Run error mitigation strategies on an OpenQASM circuit";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);
    solution.setSolutionData(input);
    return Mono.just(solution);
  }

  @Override
  public ProblemType<String, String> getProblemType() {
    return ErrorMitigationConfiguration.MITIGATION_CONFIG;
  }
}
