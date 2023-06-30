package edu.kit.provideq.toolbox;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages all solutions currently present in memory.
 */
public class SolutionManager<SolutionT> {
  private final AtomicLong nextId = new AtomicLong();

  private final List<Solution<SolutionT>> solutions = new LinkedList<>();

  public Solution<SolutionT> createSolution() {
    var id = nextId.incrementAndGet();
    Solution<SolutionT> solution = new Solution<>(id);
    solutions.add(solution);
    return solution;
  }

  public Solution<SolutionT> getSolution(long id) {
    return solutions.stream()
        .filter(s -> s.getId() == id)
        .findFirst()
        .orElse(null);
  }

  public void removeSolution(long id) {
    var solution = getSolution(id);
    if (solution != null) {
      solutions.remove(solution);
    }
  }
}
