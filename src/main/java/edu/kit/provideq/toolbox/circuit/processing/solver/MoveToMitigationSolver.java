package edu.kit.provideq.toolbox.circuit.processing.solver;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.circuit.processing.solver.mitigation.ErrorMitigationConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class MoveToMitigationSolver extends CircuitProcessingSolver {
  private static final SubRoutineDefinition<String, String> MITIGATOR_SUBROUTINE =
      new SubRoutineDefinition<>(
          ErrorMitigationConfiguration.MITIGATION_CONFIG,
          "Creates a mitigation solver"
      );

  @Override
  public String getName() {
    return "Mitigate QASM Code Errors";
  }

  @Override
  public String getDescription() {
    return "Move QASM input to the error mitigators";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(MITIGATOR_SUBROUTINE);
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    return subRoutineResolver.runSubRoutine(MITIGATOR_SUBROUTINE, input);
  }
}
