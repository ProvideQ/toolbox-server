package edu.kit.provideq.toolbox.meta;

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
   * @return the solution produced by the sub-routine.
   */
  <InputT, ResultT> Mono<Solution<ResultT>> runSubRoutine(
      SubRoutineDefinition<InputT, ResultT> subRoutine,
      InputT input
  );
}
