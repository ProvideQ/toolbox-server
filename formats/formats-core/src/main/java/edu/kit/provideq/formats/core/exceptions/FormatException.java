package edu.kit.provideq.formats.core.exceptions;

/**
 * Base exception for all errors raised by the formats library.
 */
public class FormatException extends Exception {
  public FormatException(String message) {
    super(message);
  }

  public FormatException(String message, Throwable cause) {
    super(message, cause);
  }
}
