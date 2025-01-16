package edu.kit.provideq.toolbox.exception;

import edu.kit.provideq.toolbox.meta.ProblemType;

public class MissingExampleException extends RuntimeException {
  public MissingExampleException(ProblemType<?, ?> problemType) {
    super(getMessage(problemType));
  }

  public MissingExampleException(ProblemType<?, ?> problemType, Throwable cause) {
    super(getMessage(problemType), cause);
  }

  private static String getMessage(ProblemType<?, ?> problemType) {
    return "no %s problem example available".formatted(problemType.getId());
  }
}
