package edu.kit.provideq.toolbox.meta.strategy;

import edu.kit.provideq.toolbox.meta.RuleProperty;
import edu.kit.provideq.toolbox.meta.RuleType;

public record CorrectnessViolation(
    String problemId,
    RuleType ruleType,
    RuleProperty requiredProperty
) {
}
