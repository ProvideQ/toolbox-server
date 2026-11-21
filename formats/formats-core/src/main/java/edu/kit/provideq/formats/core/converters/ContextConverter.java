package edu.kit.provideq.formats.core.converters;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.exceptions.FormatException;

/**
 * A converter whose conversion requires problem-instance context (e.g. the input document
 * that defines a variable or node order).
 *
 * <p>Unlike a plain {@link Converter}, a context converter is a stateless function of both
 * the value and the context: the context is passed to {@link #convert(Object, Object)} on
 * every invocation instead of being captured in a converter instance.
 *
 * <p>Context converters are registered with the {@link ConversionRegistry} like plain
 * converters, but they are only applied by
 * {@link ConversionService#convert(Object, Format, Format, Object)}: the caller supplies
 * the context object, the service validates it against {@link #getContextClass()} before
 * invoking the conversion.
 *
 * @param <A> source format's object representation
 * @param <B> target format's object representation
 * @param <C> context type required for the conversion
 */
public interface ContextConverter<A, B, C>  {
  /**
   * Returns the format this converter converts from.
   */
  Format<A> getSource();

  /**
   * Returns the format this converter converts to.
   */
  Format<B> getTarget();

  /**
   * Returns the class of context objects this converter accepts.
   */
  Class<C> getContextClass();

  /**
   * Converts a value from the source format's representation to the target format's,
   * using the given problem-instance context.
   *
   * @param value value in the source representation
   * @param context the problem-instance context for the conversion
   * @return value in the target representation
   * @throws FormatException if the conversion failed
   */
  B convert(A value, C context) throws FormatException;
}
