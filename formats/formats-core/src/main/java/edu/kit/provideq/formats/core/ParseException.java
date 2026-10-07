package edu.kit.provideq.formats.core;

/**
 * Thrown when input could not be parsed into a format's object representation.
 */
public class ParseException extends FormatException {
  public ParseException(String message) {
    super(message);
  }

  public ParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
