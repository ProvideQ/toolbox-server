package edu.kit.provideq.toolbox.util;

/**
 * Represents an immutable pair of two values.
 *
 * @param <T1> The type of the first element in the pair.
 * @param <T2> The type of the second element in the pair.
 */
public record Pair<T1, T2>(T1 first, T2 second) {
  public Pair<T1, T2> copyWithFirst(T1 first) {
    return new Pair<>(first, this.second);
  }

  public Pair<T1, T2> copyWithSecond(T2 second) {
    return new Pair<>(this.first, second);
  }
}
