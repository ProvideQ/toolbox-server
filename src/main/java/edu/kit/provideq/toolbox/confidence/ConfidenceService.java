package edu.kit.provideq.toolbox.confidence;

import edu.kit.provideq.toolbox.meta.ProblemSolver;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConfidenceService {

  private final List<ConfidenceCalculator> calculators;

  public ConfidenceService(List<ConfidenceCalculator> calculators) {
    this.calculators = calculators;
  }

  public Confidence getConfidence(ProblemSolver<?, ?> solver) {
    return calculators.stream()
        .filter(calc -> calc.supports(solver))
        .findFirst()                             // first matching calculator wins
        .map(calc -> calc.calculate(solver))
        .orElse(Confidence.UNKNOWN);
  }
}
