package edu.kit.provideq.toolbox.exception;

public class MissingExampleException extends RuntimeException {
  public MissingExampleException(String message) {
    super(message);
  }

  public MissingExampleException(String message, Throwable cause) {
    super(message, cause);
  }
}
