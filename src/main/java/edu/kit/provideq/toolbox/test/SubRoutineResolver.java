package edu.kit.provideq.toolbox.test;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface SubRoutineResolver {
  <InputT, ResultT> Mono<Solution<ResultT>>
    resolve(SubRoutineDefinition<InputT, ResultT> subRoutine, InputT input);
}
