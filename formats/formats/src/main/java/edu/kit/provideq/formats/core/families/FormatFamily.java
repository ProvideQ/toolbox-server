package edu.kit.provideq.formats.core.families;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import java.util.Set;

/**
 * A format family groups all formats that can express instances of a problem type.
 *
 * <p>Families are the problem-type-facing view over the format conversion graph:
 * they define which formats may be submitted for a problem type, while the conversion
 * graph defines which representations can be derived from a value. A format may belong
 * to multiple families; family membership is managed by the {@link ConversionRegistry},
 * so families only carry their identity and canonical format.
 *
 * <p>Families are usually singletons; their type is the compile-time identity used in
 * {@code ProblemType} and {@code ProblemSolver} type parameters.
 */
public interface FormatFamily {
  /**
   * Returns the canonical name of this family, e.g. "SAT".
   */
  String getName();

  /**
   * Returns the canonical (primary) format of this family, e.g. the DIMACS CNF format
   * for the SAT family.
   */
  Format<?> getCanonicalFormat();

  /**
   * Returns the formats this family is initially registered with.
   * Further formats can be added dynamically via
   * {@link ConversionRegistry#addToFamily(FormatFamily, Format)}.
   */
  default Set<Format<?>> getInitialFormats() {
    return Set.of();
  }

  /**
   * Returns attribute names supported by the family's initial formats.
   */
  default Set<String> getAttributeNames() {
    return getInitialFormats().stream()
        .flatMap(format -> format.getAttributeReaders().keySet().stream())
        .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }
}
