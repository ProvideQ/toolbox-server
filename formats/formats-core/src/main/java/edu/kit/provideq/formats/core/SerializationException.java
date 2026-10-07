package edu.kit.provideq.formats.core;

/**
 * Thrown when a format's object representation could not be serialized.
 */
public class SerializationException extends FormatException {
  public SerializationException(String message) {
    super(message);
  }

  public SerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
