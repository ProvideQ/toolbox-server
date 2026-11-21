package edu.kit.provideq.toolbox.circuit.processing.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.circuit.processing.QasmFamily;
import edu.kit.provideq.toolbox.circuit.processing.solvers.executor.ExecutionResultFamily;
import edu.kit.provideq.toolbox.circuit.processing.solvers.optimization.OptimizationConfiguration;
import edu.kit.provideq.toolbox.formats.QasmFormat;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MoveToOptimizationSolver extends CircuitProcessingSolver {
  private static final SubRoutineDefinition<QasmFamily, ExecutionResultFamily>
      OPTIMIZER_SUBROUTINE =
      SubRoutineDefinition.of(
          OptimizationConfiguration.OPTIMIZATION_CONFIG,
          "Creates an optimization solver",
          true
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
  public Mono<Solution<ExecutionResultFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var qasm = convertFormatted(formattedInput, QasmFormat.INSTANCE, conversionService);
    return subRoutineResolver.runSubRoutine(OPTIMIZER_SUBROUTINE, qasm, conversionService)
        .map(s -> Solution.from(s, x -> x));
  }
}