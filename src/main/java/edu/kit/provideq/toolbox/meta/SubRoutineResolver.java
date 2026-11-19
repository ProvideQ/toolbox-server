package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.Solution;
import reactor.core.publisher.Mono;

/**
 * This interface can be used by {@link ProblemSolver problem solvers} to
 * call their registered {@link SubRoutineDefinition sub-routines}.
 */
@FunctionalInterface
public interface SubRoutineResolver {
  /**
   * Runs the given sub-routine with the given input.
   *
   * @param subRoutine the sub-routine to run.
   *     The specified sub-routine must have been declared in
   *     {@link ProblemSolver#getSubRoutines()}.
   * @param input the input data to call the sub-routine with.
   *     The sub problem solver pulls the representations it needs from this value;
   *     representations are derived via the conversion graph if necessary.
   * @return the solution produced by the sub-routine.
   */
  <InT extends FormatFamily, OutT extends FormatFamily> Mono<Solution<OutT>> runSubRoutine(
      SubRoutineDefinition<InT, OutT> subRoutine,
      FormattedValue<?> input,
      ConversionService conversionService
  );
}