package edu.kit.provideq.formats.core;

/**
 * The degree to which a conversion preserves the constraints of the source problem.
 *
 * <p>The solution space of a problem is the set of values that satisfy all of its constraints.
 *
 * <ul>
 *   <li>{@link #STRONG}: the output solution space is a subset of the input solution space,
 *       i.e. all input constraints carry over as hard constraints.</li>
 *   <li>{@link #WEAK}: the output solution space is not necessarily a subset of the input
 *       solution space, but penalties are defined that substitute for violated input
 *       constraints.</li>
 *   <li>{@link #NONE}: constraints are not preserved and no penalties are defined.</li>
 * </ul>
 *
 * <p>{@code STRONG} implies {@code WEAK}: a conversion that keeps all constraints hard also
 * has a well-defined penalty interpretation, so wherever weak preservation suffices, a strong
 * conversion is acceptable.
 */
public enum ConstraintPreservation {
  /**
   * The output solution space is a subset of the input solution space; all input constraints
   * carry over as hard constraints.
   */
  STRONG,

  /**
   * The output solution space is not necessarily a subset of the input solution space, but
   * penalties are defined that substitute for violated input constraints.
   */
  WEAK,

  /**
   * Constraints are not preserved and no penalties are defined.
   */
  NONE
}
