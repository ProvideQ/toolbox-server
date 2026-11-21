package edu.kit.provideq.toolbox.exception;

/**
 * Unchecked exception thrown by problem solvers to signal that a conversion between formats failed.
 */
public class ConversionException extends SolverException {
  public ConversionException(String message) {
    super(message);
  }

  public ConversionException(String message, Throwable cause) {
    super(message, cause);
  }
}
