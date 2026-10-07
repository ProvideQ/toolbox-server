package edu.kit.provideq.formats.core;

/**
 * Thrown when no conversion path exists between two formats,
 * or when a property can not be read from any format reachable via conversion.
 */
public class RouteNotFoundException extends FormatException {
  public RouteNotFoundException(String message) {
    super(message);
  }
}
