package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.List;

public record ProblemTypeDto(String id, String description, List<String> attributes) {
  public static ProblemTypeDto fromProblemType(final ProblemType<?, ?> problemType) {
    return new ProblemTypeDto(
        problemType.getId(),
        problemType.getDescription(),
        problemType.getAttributes().keySet().stream().toList());
  }
}
