package edu.kit.provideq.toolbox.exception;

import edu.kit.provideq.toolbox.meta.ProblemType;

public class MissingSolverException extends RuntimeException {
  public MissingSolverException(ProblemType<?, ?> problemType) {
    super(getMessage(problemType));
  }

  public MissingSolverException(ProblemType<?, ?> problemType, Throwable cause) {
    super(getMessage(problemType), cause);
  }

  private static String getMessage(ProblemType<?, ?> problemType) {
    return "no %s problem solver available".formatted(problemType.getId());
  }
}
