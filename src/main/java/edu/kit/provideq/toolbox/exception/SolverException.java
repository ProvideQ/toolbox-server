package edu.kit.provideq.toolbox.exception;

/**
 * Unchecked exception thrown by problem solvers to signal that a problem instance could not
 * be solved.
 * When a solver throws this exception, the toolbox converts it into a failed {@code Solution}
 * so solvers do not need to catch, build and return failure solutions themselves.
 *
 * <p>For errors caused by invalid problem input use {@link ConversionException}, which
 * results in an aborted ({@code INVALID}) solution instead.
 */
public class SolverException extends RuntimeException {
  public SolverException(String message) {
    super(message);
  }

  public SolverException(String message, Throwable cause) {
    super(message, cause);
  }
}
