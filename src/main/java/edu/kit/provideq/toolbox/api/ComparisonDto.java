package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.BoundWithInfo;
import edu.kit.provideq.toolbox.Solution;

public record ComparisonDto(int comparison, BoundWithInfo bound, Solution<?> solution) {
  @Override public String toString() {
    return "Comparison{comparison=%d, bound=%s, solution=%s}"
        .formatted(comparison, bound, solution);
  }
}
