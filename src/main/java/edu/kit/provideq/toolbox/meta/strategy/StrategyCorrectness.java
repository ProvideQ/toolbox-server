package edu.kit.provideq.toolbox.meta.strategy;

import edu.kit.provideq.toolbox.meta.RuleProperty;
import edu.kit.provideq.toolbox.meta.RuleType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record StrategyCorrectness(
    CorrectnessVerdict verdict,
    List<CorrectnessViolation> violations
) {
  private static final Map<RuleType, RuleProperty> REQUIRED_PROPERTIES = Map.of(
      RuleType.SOLVE, RuleProperty.VALID,
      RuleType.REFORMULATION, RuleProperty.STRONGLY_CONSTRAINT_PRESERVING,
      RuleType.DECOMPOSITION, RuleProperty.STRONGLY_CONSTRAINT_PRESERVING
  );

  public static StrategyCorrectness evaluate(List<StrategyStep> steps) {
    var violations = new ArrayList<CorrectnessViolation>();
    var hasUnknownRules = false;

    for (var step : steps) {
      if (!step.isConfigured()) {
        hasUnknownRules = true;
        continue;
      }

      var characteristics = step.characteristics();
      for (var ruleType : characteristics.types()) {
        var requiredProperty = REQUIRED_PROPERTIES.get(ruleType);
        if (requiredProperty != null && !characteristics.properties().contains(requiredProperty)) {
          violations.add(new CorrectnessViolation(step.problemId(), ruleType, requiredProperty));
        }
      }
    }

    CorrectnessVerdict verdict;
    if (!violations.isEmpty()) {
      verdict = CorrectnessVerdict.INCORRECT;
    } else if (hasUnknownRules) {
      verdict = CorrectnessVerdict.UNDETERMINED;
    } else {
      verdict = CorrectnessVerdict.CORRECT;
    }

    return new StrategyCorrectness(verdict, List.copyOf(violations));
  }
}
