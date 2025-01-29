package edu.kit.provideq.toolbox.circuit.processing.solver;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.circuit.processing.solver.executor.ExecutionResult;
import edu.kit.provideq.toolbox.circuit.processing.solver.executor.ExecutorConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class MoveToExecutionSolver extends CircuitProcessingSolver {
  private static final SubRoutineDefinition<String, ExecutionResult> EXECUTOR_SUBROUTINE =
      new SubRoutineDefinition<>(
          ExecutorConfiguration.EXECUTOR_CONFIG,
          "Creates a execution solver"
      );

  @Override
  public String getName() {
    return "Execute QASM Code";
  }

  @Override
  public String getDescription() {
    return "Move QASM input to the executors";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(EXECUTOR_SUBROUTINE);
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    return subRoutineResolver.runSubRoutine(EXECUTOR_SUBROUTINE, input)
        .map(executionResultSolution -> {
          Solution<String> solution = new Solution<>(this);
          SolutionStatus status = executionResultSolution.getStatus();
          if (status == SolutionStatus.ERROR) {
            solution.fail();
            solution.setDebugData("An error occurred while executing the circuit");
            return solution;
          }
          solution.complete();
          solution.setSolutionData(executionResultSolution.getSolutionData().toString());
          return solution;
        });
  }
}
