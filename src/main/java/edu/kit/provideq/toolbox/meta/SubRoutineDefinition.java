package edu.kit.provideq.toolbox.meta;

/**
 * A sub-routine definition describes which problem type needs to be solved by a sub-routine and why
 * it needs to be solved.
 *
 * @param problemTypeId {@link ProblemType#getId() id} of the problem type that needs to be solved
 *     by this sub-routine.
 * @param description description of the sub-routine call to provide information where and why it is
 *     needed.
 * @see #SubRoutineDefinition(ProblemType, String)
 */
public record SubRoutineDefinition(String problemTypeId, String description) {
  /**
   * Creates a sub-routine definition for a given problem type with a given description.
   *
   * @param type problem type that needs to be solved by this sub-routine.
   * @param description description of the sub-routine call to provide information where and why it
   *     is needed.
   */
  public SubRoutineDefinition(ProblemType type, String description) {
    this(type.getId(), description);
  }
}
