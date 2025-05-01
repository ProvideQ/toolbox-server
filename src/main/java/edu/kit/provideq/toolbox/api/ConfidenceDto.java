package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.confidence.Confidence;
import java.util.Map;

public record ConfidenceDto(Double score, Map<String, String> factors) {
  public ConfidenceDto(Confidence confidence) {
    this(confidence.score().orElse(null), confidence.factors());
  }
}
