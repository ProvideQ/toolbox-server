package edu.kit.provideq.toolbox.circuit.processing.solver.executor;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class ExecutionSolver implements ProblemSolver<String, ExecutionResult> {
  @Override
  public String getName() {
    return "Execute OpenQASM circuit";
  }

  @Override
  public String getDescription() {
    return "Execute an OpenQASM circuit";
  }

  @Override
  public Mono<Solution<ExecutionResult>> solve(String input, SubRoutineResolver subRoutineResolver, SolvingProperties properties) {
    var solution = new Solution<>(this);
    solution.setSolutionData(new ExecutionResult(Optional.of(input), Optional.empty()));
    return Mono.just(solution);
  }

  @Override
  public ProblemType<String, ExecutionResult> getProblemType() {
    return ExecutorConfiguration.EXECUTOR_CONFIG;
  }
}
