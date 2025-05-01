package edu.kit.provideq.toolbox.confidence;

import edu.kit.provideq.toolbox.meta.ProblemSolver;

public interface ConfidenceCalculator {
  boolean supports(ProblemSolver<?, ?> solver);

  Confidence calculate(ProblemSolver<?, ?> solver);
}
