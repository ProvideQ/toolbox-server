package edu.kit.provideq.toolbox.meta;

import java.util.Set;

public enum RuleProperty {
  EXACT(RuleType.SOLVE),
  VALID(RuleType.SOLVE),
  STRONGLY_CONSTRAINT_PRESERVING(
      RuleType.REFORMULATION, RuleType.DECOMPOSITION, RuleType.DELEGATION),
  WEAKLY_CONSTRAINT_PRESERVING(
      RuleType.REFORMULATION, RuleType.DECOMPOSITION, RuleType.DELEGATION),
  OPTIMAL_SOLUTION_PRESERVING(
      RuleType.REFORMULATION, RuleType.DECOMPOSITION, RuleType.DELEGATION);

  private final Set<RuleType> applicableTo;

  RuleProperty(RuleType... applicableTo) {
    this.applicableTo = Set.of(applicableTo);
  }

  public Set<RuleType> getApplicableTo() {
    return applicableTo;
  }

  public boolean isApplicableTo(RuleType ruleType) {
    return applicableTo.contains(ruleType);
  }
}
