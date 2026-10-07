package edu.kit.provideq.formats.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of formats and converters.
 *
 * <p>Whenever a converter is registered, all reachable conversion paths are pre-computed
 * (registration-time cost), so that runtime route lookups are O(1).
 *
 * <p>The registry is not thread-safe during registration; call {@link #seal} afterward or
 * perform registrations before handing the registry to other threads.
 */
public class ConversionRegistry {
  private final Map<String, Format<?>> formats = new HashMap<>();
  private final Map<Format<?>, List<Converter<?, ?>>> outgoing = new HashMap<>();
  private final Map<Format<?>, Map<Format<?>, List<Converter<?, ?>>>> shortestPaths = new HashMap<>();
  private boolean sealed = false;

  /**
   * Registers a format.
   *
   * @param format format to register
   * @return the registered format
   * @throws IllegalStateException if the registry is sealed
   */
  public <T> Format<T> register(Format<T> format) {
    requireOpen();
    Objects.requireNonNull(format, "format");

    for (var alias : format.getAliases()) {
      formats.put(alias, format);
    }
    formats.put(format.getName(), format);
    outgoing.put(format, new ArrayList<>());

    return format;
  }

  /**
   * Registers a converter and updates the pre-computed conversion paths.
   *
   * @param converter converter to register
   * @throws IllegalStateException if the registry is sealed
   */
  public <A, B> void register(Converter<A, B> converter) {
    requireOpen();
    Objects.requireNonNull(converter, "converter");

    var source = converter.getSource();
    var target = converter.getTarget();

    if (!outgoing.containsKey(source)) {
      register(source);
    }
    if (!outgoing.containsKey(target)) {
      register(target);
    }

    outgoing.get(source).add(converter);

    // The new edge can extend paths from any format that reaches this edge's source
    // (including the source itself).
    recomputePathsFrom(source);
    for (var format : outgoing.keySet()) {
      if (!format.equals(source)) {
        var pathMap = shortestPaths.get(format);
        if (pathMap != null && pathMap.containsKey(source)) {
          recomputePathsFrom(format);
        }
      }
    }
  }

  /**
   * Convenience method for registering a converter and its inverse in one call.
   *
   * @param converter converter to register
   * @param inverse the inverse converter
   */
  public <A, B> void registerBidirectional(Converter<A, B> converter, Converter<B, A> inverse) {
    register(converter);
    register(inverse);
  }

  /**
   * Seals the registry: no further registrations are allowed.
   * Implementations may use this to finish pre-computation.
   */
  public void seal() {
    this.sealed = true;
  }

  /**
   * Returns the format registered under the given name or alias.
   *
   * @param name name or alias of the format
   * @return the format, or {@code null} if not registered
   */
  @SuppressWarnings("unchecked")
  public <T> Format<T> getFormat(String name) {
    return (Format<T>) formats.get(Objects.requireNonNull(name, "name"));
  }

  /**
   * Returns all registered converters leaving the given format.
   */
  public List<Converter<?, ?>> getOutgoing(Format<?> format) {
    var converters = outgoing.get(format);
    return converters == null ? List.of() : Collections.unmodifiableList(converters);
  }

  /**
   * Returns the pre-computed shortest conversion path from one format to another.
   * Prefers lossless converters when multiple paths of equal length exist.
   *
   * @param from source format
   * @param to target format
   * @return the list of converters to apply in order, or {@code null} if no path exists
   */
  public List<Converter<?, ?>> getPath(Format<?> from, Format<?> to) {
    if (from.equals(to)) {
      return List.of();
    }

    var pathMap = shortestPaths.get(from);
    if (pathMap == null) {
      return List.of();
    }
    return pathMap.get(to);
  }

  private void requireOpen() {
    if (sealed) {
      throw new IllegalStateException("registry is sealed");
    }
  }

  /**
   * Recomputes shortest paths starting at the given source format.
   * Uses BFS over the converter graph; among equal-length paths, lossless edges win.
   */
  private void recomputePathsFrom(Format<?> source) {
    var pathMap = new HashMap<Format<?>, List<Converter<?, ?>>>();
    var visited = new HashMap<Format<?>, Boolean>();

    var frontier = new ArrayList<List<Converter<?, ?>>>();
    frontier.add(List.of());
    visited.put(source, true);

    while (!frontier.isEmpty()) {
      var nextFrontier = new ArrayList<List<Converter<?, ?>>>();

      for (var path : frontier) {
        var current = path.isEmpty() ? source : path.getLast().getTarget();

        for (var converter : outgoing.get(current)) {
          var next = converter.getTarget();
          if (visited.containsKey(next)) {
            continue;
          }

          var newPath = new ArrayList<>(path);
          newPath.add(converter);
          addIfBetter(pathMap, next, newPath);
          nextFrontier.add(newPath);
        }
      }

      for (var path : nextFrontier) {
        visited.put(path.getLast().getTarget(), true);
      }
      frontier = nextFrontier;
    }

    shortestPaths.put(source, pathMap);
  }

  /**
   * Among paths of equal length to the same target, prefers the lossless one.
   */
  private void addIfBetter(
      Map<Format<?>, List<Converter<?, ?>>> pathMap,
      Format<?> target,
      List<Converter<?, ?>> path) {
    var existing = pathMap.get(target);
    if (existing != null && isMoreLossless(existing, path)) {
      return;
    }
    pathMap.put(target, List.copyOf(path));
  }

  private boolean isMoreLossless(List<Converter<?, ?>> existing, List<Converter<?, ?>> candidate) {
    var existingLossy = existing.stream().filter(c -> !c.isLossless()).count();
    var candidateLossy = candidate.stream().filter(c -> !c.isLossless()).count();
    return existingLossy <= candidateLossy;
  }

  /**
   * Returns the pre-computed path index for the given source format.
   * Intended for tests and diagnostics.
   */
  public Map<Format<?>, List<Converter<?, ?>>> getPathsFrom(Format<?> source) {
    var pathMap = shortestPaths.get(source);
    return pathMap == null ? Map.of() : Collections.unmodifiableMap(pathMap);
  }
}
