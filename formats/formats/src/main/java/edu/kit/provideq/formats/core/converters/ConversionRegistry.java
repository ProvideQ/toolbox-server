package edu.kit.provideq.formats.core.converters;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
  private final List<Converter<?, ?>> converters = new ArrayList<>();
  private final List<ContextConverter<?, ?, ?>> contextConverters = new ArrayList<>();
  private final Map<Format<?>, Map<Format<?>, List<ConversionPath>>> paths = new HashMap<>();
  private final Map<String, FormatFamily> families = new HashMap<>();
  private final Map<String, Set<Format<?>>> familyMembers = new HashMap<>();
  private boolean sealed = false;

  /**
   * A visited BFS state: a format reached with a path requiring the given context type.
   */
  private record PathKey(Format<?> format, Class<?> contextType) {
  }

  /**
   * A BFS queue entry: a visited state plus the path steps leading to it.
   */
  private record PathEntry(PathKey key, List<Object> steps) {
  }

  /**
   * Registers a format.
   *
   * @param format format to register
   * @return the registered format
   * @throws IllegalStateException if the registry is sealed
   */
  public <T> Format<T> registerFormat(Format<T> format) {
    requireOpen();
    Objects.requireNonNull(format, "format");

    if (outgoing.containsKey(format)) {
      // already registered: keep the existing outgoing converters intact
      return format;
    }

    for (var alias : format.getAliases()) {
      formats.put(alias, format);
    }
    formats.put(format.getName(), format);
    outgoing.put(format, new ArrayList<>());

    return format;
  }

  /**
   * Registers a format family and its initial formats.
   *
   * @param family family to register
   * @throws IllegalStateException if the registry is sealed
   * @throws IllegalStateException if a family with the same name is already registered
   */
  public void registerFormat(FormatFamily family) {
    requireOpen();
    Objects.requireNonNull(family, "family");

    if (families.containsKey(family.getName())) {
      throw new IllegalStateException(
          "A family with name %s is already registered".formatted(family.getName()));
    }
    families.put(family.getName(), family);
    familyMembers.put(family.getName(), new HashSet<>());

    for (var format : family.getInitialFormats()) {
      if (!outgoing.containsKey(format)) {
        registerFormat(format);
      }
      familyMembers.get(family.getName()).add(format);
    }
  }

  /**
   * Registers a converter and updates the pre-computed conversion paths.
   *
   * @param converter converter to register
   * @throws IllegalStateException if the registry is sealed
   */
  public <A, B> void registerConverter(Converter<A, B> converter) {
    requireOpen();
    Objects.requireNonNull(converter, "converter");

    var source = converter.getSource();
    var target = converter.getTarget();

    if (!outgoing.containsKey(source)) {
      registerFormat(source);
    }
    if (!outgoing.containsKey(target)) {
      registerFormat(target);
    }

    converters.add(converter);
    outgoing.get(source).add(converter);

    // The new edge can extend paths from any format; recompute the full index
    recomputeAllPaths();
  }

  /**
   * Registers a context converter and updates the pre-computed conversion paths.
   * Context converter edges are only traversable when a matching context is supplied to
   * {@link ConversionService#convert(Object, Format, Format, Object)}; plain lookups via
   * {@link #getPath(Format, Format)} never route through them.
   *
   * @param contextConverter context converter to register
   * @throws IllegalStateException if the registry is sealed
   */
  public <A, B, C> void registerContextConverter(ContextConverter<A, B, C> contextConverter) {
    requireOpen();
    Objects.requireNonNull(contextConverter, "contextConverter");

    var source = contextConverter.getSource();
    var target = contextConverter.getTarget();

    if (!outgoing.containsKey(source)) {
      registerFormat(source);
    }
    if (!outgoing.containsKey(target)) {
      registerFormat(target);
    }

    contextConverters.add(contextConverter);
    recomputeAllPaths();
  }

  /**
   * Convenience method for registering a converter and its inverse in one call.
   *
   * @param converter converter to register
   * @param inverse the inverse converter
   */
  public <A, B> void registerBidirectional(Converter<A, B> converter, Converter<B, A> inverse) {
    registerConverter(converter);
    registerConverter(inverse);
  }

  /**
   * Seals the registry: no further registrations are allowed.
   * Implementations may use this to finish pre-computation.
   */
  public void seal() {
    this.sealed = true;
  }

  /**
   * Adds a format to a registered family, registering the format if it is unknown.
   * The format should have a conversion path to the family's other members; this is
   * validated by family coherence tests.
   *
   * @param family family to extend
   * @param format format to add to the family
   * @throws IllegalStateException if the registry is sealed
   * @throws IllegalArgumentException if the family is not registered
   */
  public void addToFamily(FormatFamily family, Format<?> format) {
    requireOpen();
    Objects.requireNonNull(family, "family");
    Objects.requireNonNull(format, "format");

    var members = familyMembers.get(family.getName());
    if (members == null) {
      throw new IllegalArgumentException(
          "Family %s is not registered".formatted(family.getName()));
    }
    registerFormat(format);
    members.add(format);
  }

  /**
   * Returns the format family registered under the given name.
   *
   * @param name name of the family
   * @return the family, or {@code null} if not registered
   */
  public FormatFamily getFamily(String name) {
    return families.get(Objects.requireNonNull(name, "name"));
  }

  /**
   * Returns all registered format families.
   */
  public Collection<FormatFamily> getFamilies() {
    return Collections.unmodifiableCollection(families.values());
  }

  /**
   * Returns the formats currently registered as members of the given family.
   */
  public Set<Format<?>> getFormats(FormatFamily family) {
    var members = familyMembers.get(family.getName());
    return members == null ? Set.of() : Collections.unmodifiableSet(members);
  }

  /**
   * Returns all registered formats.
   */
  public Collection<Format<?>> getFormats() {
    return Collections.unmodifiableCollection(outgoing.keySet());
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
   * Returns all registered converters that don't require a context object.
   */
  public List<Converter<?, ?>> getConverters() {
    return Collections.unmodifiableList(converters);
  }

  /**
   * Returns all registered context converters.
   */
  public List<ContextConverter<?, ?, ?>> getContextConverters() {
    return Collections.unmodifiableList(contextConverters);
  }

  /**
   * Returns the pre-computed shortest context-free conversion path from one format to
   * another.
   *
   * @param from source format
   * @param to target format
   * @return the shortest context-free path, or {@code null} if none exists
   */
  public ConversionPath getPath(Format<?> from, Format<?> to) {
    if (from.equals(to)) {
      return new ConversionPath(List.of(), null);
    }

    var pathList = paths.getOrDefault(from, Map.of()).get(to);
    if (pathList == null) {
      return null;
    }
    for (var path : pathList) {
      if (path.isPlain()) {
        return path;
      }
    }
    return null;
  }

  /**
   * Returns the pre-computed shortest conversion path from one format to another that is
   * compatible with the given context: context-free paths and paths whose required
   * context type matches the context's type both qualify. The path lists are sorted by
   * length, so the returned path is the best one the context can be applied to.
   *
   * @param from source format
   * @param to target format
   * @param context the context object available for context steps
   * @return the shortest compatible path, or {@code null} if none exists
   */
  public ConversionPath getPath(Format<?> from, Format<?> to, Object context) {
    if (from.equals(to)) {
      return new ConversionPath(List.of(), null);
    }

    var pathList = paths.getOrDefault(from, Map.of()).get(to);
    if (pathList == null) {
      return null;
    }
    for (var path : pathList) {
      if (path.isPlain() || path.contextType().isInstance(context)) {
        return path;
      }
    }
    return null;
  }

  /**
   * Returns all pre-computed conversion paths from one format to another, sorted by
   * length: context-free paths and paths requiring a single context object.
   *
   * @param from source format
   * @param to target format
   * @return the paths in ascending length order, empty if the target is unreachable
   */
  public List<ConversionPath> getPaths(Format<?> from, Format<?> to) {
    return paths.getOrDefault(from, Map.of()).getOrDefault(to, List.of());
  }

  private void requireOpen() {
    if (sealed) {
      throw new IllegalStateException("registry is sealed");
    }
  }

  /**
   * Recomputes the full path index: for every source format, the shortest path to every
   * reachable format per required context type.
   *
   * <p>Uses BFS over states of (format, required context type): plain edges keep the
   * current context requirement, context edges add their own. Paths that would require
   * multiple different contexts are pruned, as they cannot be applied with a single
   * context object.
   */
  private void recomputeAllPaths() {
    paths.clear();
    for (var source : outgoing.keySet()) {
      recomputePathsFrom(source);
    }
  }

  /**
   * Computes the path index for paths starting at the given source format.
   */
  private void recomputePathsFrom(Format<?> source) {
    var pathMap = new HashMap<Format<?>, List<ConversionPath>>();
    var visited = new HashSet<PathKey>();
    var queue = new ArrayDeque<PathEntry>();

    var startKey = new PathKey(source, null);
    visited.add(startKey);
    queue.add(new PathEntry(startKey, List.of()));

    while (!queue.isEmpty()) {
      var entry = queue.poll();
      var key = entry.key();

      // each (target, context type) state is discovered once, at its shortest path length
      pathMap.computeIfAbsent(key.format(), k -> new ArrayList<>())
          .add(new ConversionPath(entry.steps(), key.contextType()));

      for (var converter : outgoing.get(key.format())) {
        var nextKey = new PathKey(converter.getTarget(), key.contextType());
        if (visited.add(nextKey)) {
          queue.add(new PathEntry(nextKey, appendStep(entry.steps(), converter)));
        }
      }

      for (var contextConverter : contextConverters) {
        if (!contextConverter.getSource().equals(key.format())) {
          continue;
        }
        var required = mergeContextType(key.contextType(), contextConverter.getContextClass());
        if (required == null) {
          // the path would require multiple different contexts: not applicable
          continue;
        }
        var nextKey = new PathKey(contextConverter.getTarget(), required);
        if (visited.add(nextKey)) {
          queue.add(new PathEntry(nextKey, appendStep(entry.steps(), contextConverter)));
        }
      }
    }

    // BFS discovers states in non-decreasing length per state, but a target's list
    // mixes context types; sort so the best (shortest) path comes first
    for (var list : pathMap.values()) {
      list.sort(Comparator.comparingInt(path -> path.steps().size()));
    }
    paths.put(source, pathMap);
  }

  /**
   * Returns a copy of the steps with the given step appended.
   */
  private static List<Object> appendStep(List<Object> steps, Object step) {
    var copy = new ArrayList<Object>(steps);
    copy.add(step);
    return List.copyOf(copy);
  }

  /**
   * Merges the context requirement of a path with the context type of an additional
   * context step.
   *
   * @param current the path's context requirement so far, {@code null} if context-free
   * @param required the context type of the additional context step
   * @return the merged requirement, or {@code null} if the requirements conflict (the
   *         path would require two different context objects)
   */
  private static Class<?> mergeContextType(Class<?> current, Class<?> required) {
    if (current == null) {
      return required;
    }
    return current.equals(required) ? current : null;
  }

  /**
   * Returns the best context-free path from the given source format to every reachable
   * format.
   * Intended for tests and diagnostics.
   */
  public Map<Format<?>, ConversionPath> getPathsFrom(Format<?> source) {
    var pathMap = paths.get(source);
    if (pathMap == null) {
      return Map.of();
    }

    var result = new HashMap<Format<?>, ConversionPath>();
    for (var entry : pathMap.entrySet()) {
      for (var path : entry.getValue()) {
        if (path.isPlain()) {
          result.put(entry.getKey(), path);
          break;
        }
      }
    }
    return Collections.unmodifiableMap(result);
  }
}
