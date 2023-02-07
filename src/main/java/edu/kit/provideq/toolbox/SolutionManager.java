package edu.kit.provideq.toolbox;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages all solutions currently present in memory
 */
public abstract class SolutionManager {
  private static final AtomicLong NEXT_ID = new AtomicLong();

  private static final List<Solution<?>> SOLUTIONS = new LinkedList<>();

  public static <T> Solution<T> createSolution() {
    var id = SolutionManager.NEXT_ID.incrementAndGet();
    Solution<T> solution = new Solution<>(id);
    SolutionManager.SOLUTIONS.add(solution);
    return solution;
  }

  public static Solution<?> getSolution(long id) {
    return SOLUTIONS.stream()
        .filter(s -> s.getId() == id)
        .findFirst()
        .orElse(null);
  }

  public static void removeSolution(long id) {
    var solution = getSolution(id);
    if (solution != null) SOLUTIONS.remove(solution);
  }
}
