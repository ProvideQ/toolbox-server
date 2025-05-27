package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.meta.ProblemType;

public record ProblemTypeDto(String id, String description) {
  public static ProblemTypeDto fromProblemType(final ProblemType<?, ?> problemType) {
    return new ProblemTypeDto(problemType.getId(), problemType.getDescription());
  }
}
