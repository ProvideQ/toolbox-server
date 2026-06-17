package edu.kit.provideq.toolbox.circuit.processing.solver.mitigation;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.circuit.processing.results.Result;
import edu.kit.provideq.toolbox.circuit.processing.results.StringResult;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ErrorMitigationSolver implements ProblemSolver<String, Result> {

  @Override
  public String getName() {
    return "Mitigate Errors for OpenQASM";
  }

  @Override
  public String getDescription() {
    return "Run error mitigation strategies on an OpenQASM circuit";
  }

  @Override
  public Mono<Solution<Result>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);
    solution.setSolutionData(new StringResult(input));
    solution.complete();
    return Mono.just(solution);
  }

  @Override
  public ProblemType<String, Result> getProblemType() {
    return ErrorMitigationConfiguration.MITIGATION_CONFIG;
  }
}
