package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.strategy.StrategyCorrectness;
import edu.kit.provideq.toolbox.meta.strategy.StrategyStep;
import java.util.List;

public record StrategyDto(
    List<StrategyStep> steps,
    StrategyCorrectness correctness
) {
  public static StrategyDto fromProblem(Problem<?, ?> problem) {
    var steps = StrategyStep.collect(problem);
    return new StrategyDto(steps, StrategyCorrectness.evaluate(steps));
  }
}
