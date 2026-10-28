package edu.kit.provideq.formats.core;

import java.util.Objects;

/**
 * A converter implementation backed by a lambda conversion function.
 *
 * <p>Use this for simple converters instead of writing anonymous classes:
 * <pre>{@code
 *   var converter = new FuncConverter<>(
 *       ExpressionFormat.INSTANCE, DimacsCnfFormat.INSTANCE,
 *       ConstraintPreservation.STRONG, true,
 *       expression -> DimacsCnf.fromLogicalExpressionString(expression));
 * }</pre>
 *
 * @param <A> source format's object representation
 * @param <B> target format's object representation
 */
public final class FuncConverter<A, B> implements Converter<A, B> {
  private final Format<A> source;
  private final Format<B> target;
  private final ConstraintPreservation constraintPreservation;
  private final boolean optimalSolutionPreserving;
  private final Converter.ThrowingFunction<A, B> function;

  /**
   * Creates a converter backed by the given conversion function without optimal solution
   * preservation.
   *
   * @param source format the converter converts from
   * @param target format the converter converts to
   * @param constraintPreservation the degree to which the conversion preserves constraints
   * @param function the conversion function; it should throw {@link FormatException}
   *        on failure
   */
  public FuncConverter(Format<A> source, Format<B> target,
      ConstraintPreservation constraintPreservation, Converter.ThrowingFunction<A, B> function) {
    this(source, target, constraintPreservation, false, function);
  }

  /**
   * Creates a converter backed by the given conversion function.
   *
   * @param source format the converter converts from
   * @param target format the converter converts to
   * @param constraintPreservation the degree to which the conversion preserves constraints
   * @param optimalSolutionPreserving whether optimal solution preservation holds: at least one
   *        optimal solution remains in the output solution space
   * @param function the conversion function; it should throw {@link FormatException}
   *        on failure
   */
  public FuncConverter(Format<A> source, Format<B> target,
      ConstraintPreservation constraintPreservation, boolean optimalSolutionPreserving,
      Converter.ThrowingFunction<A, B> function) {
    this.source = Objects.requireNonNull(source, "source");
    this.target = Objects.requireNonNull(target, "target");
    this.constraintPreservation = Objects.requireNonNull(constraintPreservation,
        "constraintPreservation");
    this.optimalSolutionPreserving = optimalSolutionPreserving;
    this.function = Objects.requireNonNull(function, "function");
  }

  @Override
  public Format<A> getSource() {
    return source;
  }

  @Override
  public Format<B> getTarget() {
    return target;
  }

  @Override
  public ConstraintPreservation getConstraintPreservation() {
    return constraintPreservation;
  }

  @Override
  public boolean isOptimalSolutionPreserving() {
    return optimalSolutionPreserving;
  }

  @Override
  public B convert(A value) throws FormatException {
    return function.apply(value);
  }
}
