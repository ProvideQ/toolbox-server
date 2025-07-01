package edu.kit.provideq.toolbox.meta;

/**
 * A sub-routine definition describes which problem type needs to be solved by a sub-routine and why
 * it needs to be solved.
 *
 * @param type {@link ProblemType} that needs to be solved
 *     by this sub-routine.
 * @param description description of the sub-routine call to provide information where and why it is
 *     needed.
 */
public record SubRoutineDefinition<InputT, ResultT>(
    ProblemType<InputT, ResultT> type,
    String description,
    boolean isCalledOnlyOnce
) {
}
