package edu.kit.provideq.formats.core;

/**
 * Thrown when an unknown property is requested from a format.
 */
public class PropertyNotFoundException extends FormatException {
  public PropertyNotFoundException(String message) {
    super(message);
  }
}
