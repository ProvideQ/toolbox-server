package edu.kit.provideq.toolbox.confidence;

import java.util.Map;
import java.util.Optional;

public record Confidence(Optional<Double> score, Map<String, String> factors) {

  public static final Confidence UNKNOWN =
      new Confidence(Optional.empty(), Map.of("reason", "no calculator registered"));

  public boolean isUnknown() {
    return score.isEmpty();
  }

  public double numericOrMinusOne() {
    return score.orElse(-1.0);
  }
}
