package edu.kit.provideq.formats.core.families;

import edu.kit.provideq.formats.core.Format;

/**
 * Marker for formats whose conversion to or from their family's other members requires
 * problem-instance context (e.g. the input document that defines a variable or node order).
 *
 * <p>Such conversions are performed by context converters
 * ({@link edu.kit.provideq.formats.core.converters.ContextConverter}) that are registered
 * with the {@link edu.kit.provideq.formats.core.converters.ConversionRegistry} like plain
 * converters, but are only applied when
 * {@link edu.kit.provideq.formats.core.converters.ConversionService#convert(Object,
 * Format, Format, Object)} is called with the required context. Family coherence validation
 * ({@link FamilyCoherence}) accepts such context-connected members: a pair counts as
 * connected when a plain or single-context conversion path exists in either direction,
 * but they are otherwise full family members.
 */
public interface ContextBoundFormat {
}
