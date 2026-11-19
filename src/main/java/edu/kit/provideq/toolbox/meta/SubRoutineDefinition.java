package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Objects;

/**
 * A sub-routine definition describes which problem type needs to be solved by a sub-routine and
 * why it needs to be solved.
 *
 * <p>Sub-routine inputs are passed as {@link edu.kit.provideq.formats.core.FormattedValue};
 * the value carries the format it is expressed in, and the sub-routine resolver converts it
 * to a representation the sub problem solver can consume if needed.
 *
 * @param type             {@link ProblemType} that needs to be solved by this sub-routine.
 * @param description      description of the sub-routine call to provide information where
 *                         and why it is needed.
 * @param isCalledOnlyOnce whether this sub-routine is called only once during the solution
 *                         or if it may be called multiple times.
 * @param <InT>  input format family of the sub problem type.
 * @param <OutT> output format family of the sub problem type.
 */
public record SubRoutineDefinition<InT extends FormatFamily, OutT extends FormatFamily>(
    ProblemType<InT, OutT> type,
    String description,
    boolean isCalledOnlyOnce
) {
  public SubRoutineDefinition {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(description, "description");
  }

  /**
   * Creates a sub-routine definition.
   *
   * @param type             {@link ProblemType} that needs to be solved by this sub-routine.
   * @param description      description of the sub-routine call.
   * @param isCalledOnlyOnce whether this sub-routine is called only once.
   * @param <InT>  input format family of the sub problem type.
   * @param <OutT> output format family of the sub problem type.
   * @return the sub-routine definition
   */
  public static <InT extends FormatFamily, OutT extends FormatFamily>
      SubRoutineDefinition<InT, OutT> of(
          ProblemType<InT, OutT> type,
          String description,
          boolean isCalledOnlyOnce
  ) {
    return new SubRoutineDefinition<>(type, description, isCalledOnlyOnce);
  }
}
