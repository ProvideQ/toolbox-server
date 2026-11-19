package edu.kit.provideq.formats.core.converters;

import java.util.List;

/**
 * A pre-computed conversion path: the ordered steps to apply plus the context type the
 * path requires.
 *
 * <p>The steps contain {@link Converter} elements for plain steps and
 * {@link ContextConverter} elements for context steps; only the
 * {@link ConversionService} may apply them. A path's {@code contextType} is {@code null}
 * if it is context-free, or the class of the single context object required by all of its
 * context steps; paths requiring multiple different contexts are not indexed.
 *
 * @param steps the path steps to apply in order
 * @param contextType the required context type, or {@code null} for context-free paths
 */
public record ConversionPath(List<Object> steps, Class<?> contextType) {
  public ConversionPath {
    steps = List.copyOf(steps);
  }

  /**
   * Returns whether the path is context-free.
   */
  public boolean isPlain() {
    return contextType == null;
  }
}
