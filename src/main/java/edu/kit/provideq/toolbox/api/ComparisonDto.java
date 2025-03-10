package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.BoundWithInfo;
import edu.kit.provideq.toolbox.Solution;

public record ComparisonDto(float comparison, BoundWithInfo bound, Solution<?> solution) {
  @Override public String toString() {
    return "Comparison{comparison=%f, bound=%s, solution=%s}"
        .formatted(comparison, bound, solution);
  }
}
