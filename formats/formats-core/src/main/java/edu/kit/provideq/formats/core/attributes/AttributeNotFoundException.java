package edu.kit.provideq.formats.core.attributes;

import edu.kit.provideq.formats.core.exceptions.FormatException;

/**
 * Thrown when an unknown attribute is requested from a format.
 */
public class AttributeNotFoundException extends FormatException {
  public AttributeNotFoundException(String message) {
    super(message);
  }
}
