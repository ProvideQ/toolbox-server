package edu.kit.provideq.toolbox.solvers.circuit_processing.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.qasm.QasmFamily;
import edu.kit.provideq.formats.qasm.formats.QasmFormat;
import edu.kit.provideq.toolbox.meta.RuleType;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.meta.SolverCharacteristics;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.solvers.circuit_processing.formats.ExecutionResultFamily;
import edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor.ExecutorConfiguration;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MoveToExecutionSolver extends CircuitProcessingSolver {
  private static final SubRoutineDefinition<QasmFamily, ExecutionResultFamily>
      EXECUTOR_SUBROUTINE =
      SubRoutineDefinition.of(
          ExecutorConfiguration.EXECUTOR_CONFIG,
          "Creates an execution solver",
          true
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
  public SolverCharacteristics getCharacteristics() {
    return SolverCharacteristics.of(RuleType.DELEGATION);
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(EXECUTOR_SUBROUTINE);
  }

  @Override
  public Mono<Solution<ExecutionResultFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var qasm = convertFormatted(formattedInput, QasmFormat.INSTANCE, conversionService);
    return subRoutineResolver.runSubRoutine(EXECUTOR_SUBROUTINE, qasm, conversionService)
        .map(s -> Solution.from(s, x -> x));
  }
}
