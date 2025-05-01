package edu.kit.provideq.toolbox.confidence;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import org.springframework.stereotype.Component;

@Component
public class UnknownConfidenceCalculator implements ConfidenceCalculator {
  @Override public boolean supports(ProblemSolver<?, ?> solver) {
    return true;
  }

  @Override public Confidence calculate(ProblemSolver<?, ?> solver) {
    return Confidence.UNKNOWN;
  }
}
