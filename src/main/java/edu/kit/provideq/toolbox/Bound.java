package edu.kit.provideq.toolbox;

/**
 * Represents a bound value with its type.
 *
 * @param value the estimated value
 * @param boundType the type of the bound
 */
public record Bound(float value, BoundType boundType) {
}
