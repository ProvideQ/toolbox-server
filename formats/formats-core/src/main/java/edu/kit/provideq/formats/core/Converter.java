package edu.kit.provideq.formats.core;

/**
 * A converter is a directed edge in the conversion graph from source format to target format.
 *
 * @param <A> source format's object representation
 * @param <B> target format's object representation
 */
public interface Converter<A, B> {
  /**
   * Returns the format this converter converts from.
   */
  Format<A> getSource();

  /**
   * Returns the format this converter converts to.
   */
  Format<B> getTarget();

  /**
   * Returns whether the conversion preserves all information.
   */
  boolean isLossless();

  /**
   * Converts a value from the source format's representation to the target format's.
   *
   * @param value value in the source representation
   * @return value in the target representation
   * @throws FormatException if the conversion failed
   */
  B convert(A value) throws FormatException;

  /**
   * A conversion function that may throw a {@link FormatException}.
   *
   * @param <A> source representation type
   * @param <B> target representation type
   */
  @FunctionalInterface
  interface ThrowingFunction<A, B> {
    /**
     * Applies the conversion.
     *
     * @param value value in the source representation
     * @return value in the target representation
     * @throws FormatException if the conversion failed
     */
    B apply(A value) throws FormatException;
  }
}
