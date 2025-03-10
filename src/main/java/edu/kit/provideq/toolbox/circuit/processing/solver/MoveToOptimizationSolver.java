package edu.kit.provideq.toolbox.circuit.processing.solver;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.circuit.processing.solver.optimization.OptimizationConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MoveToOptimizationSolver extends CircuitProcessingSolver {
  private static final SubRoutineDefinition<String, String> OPTIMIZER_SUBROUTINE =
      new SubRoutineDefinition<>(
          OptimizationConfiguration.OPTIMIZATION_CONFIG,
          "Creates a optimization solver"
      );

  @Override
  public String getName() {
    return "Optimize QASM Code";
  }

  @Override
  public String getDescription() {
    return "Move QASM input to the optimizers";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(OPTIMIZER_SUBROUTINE);
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    return subRoutineResolver.runSubRoutine(OPTIMIZER_SUBROUTINE, input);
  }
}
