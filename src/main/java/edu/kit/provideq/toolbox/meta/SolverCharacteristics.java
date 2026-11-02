package edu.kit.provideq.toolbox.meta;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record SolverCharacteristics(
    Set<RuleType> types,
    Set<RuleProperty> properties
) {
  public SolverCharacteristics {
    if (types == null || types.isEmpty()) {
      throw new IllegalArgumentException("A solver must declare at least one rule type");
    }

    types = immutableCopy(types, RuleType.class);
    properties = immutableCopy(properties, RuleProperty.class);

    for (var property : properties) {
      if (types.stream().noneMatch(property::isApplicableTo)) {
        throw new IllegalArgumentException(
            "Property " + property + " not defined for rule types " + types
                + ", only applies to " + property.getApplicableTo());
      }
    }
  }

  public static SolverCharacteristics of(RuleType type, RuleProperty... properties) {
    return new SolverCharacteristics(Set.of(type), Set.of(properties));
  }

  private static <T extends Enum<T>> Set<T> immutableCopy(
      Collection<T> values, Class<T> enumClass) {
    var copy = EnumSet.noneOf(enumClass);
    if (values != null) {
      copy.addAll(values);
    }
    return Collections.unmodifiableSet(copy);
  }
}
