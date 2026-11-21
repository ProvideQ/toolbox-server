package edu.kit.provideq.formats.knapsack;

import java.util.List;

/**
 * Object representation of a knapsack problem.
 *
 * @param items    the items to choose from
 * @param capacity the maximum total weight of the chosen items
 */
public record KnapsackProblem(List<KnapsackItem> items, int capacity) {
  public KnapsackProblem {
    items = List.copyOf(items);
  }

  /**
   * Returns the problem in the knapsack format notation: the item count, one line per item
   * in {@code <id> <value> <weight>} notation and the capacity.
   */
  @Override
  public String toString() {
    var builder = new StringBuilder();
    builder.append(items.size()).append(System.lineSeparator());
    for (var item : items) {
      builder.append(item.id())
          .append(' ')
          .append(item.value())
          .append(' ')
          .append(item.weight())
          .append(System.lineSeparator());
    }
    builder.append(capacity).append(System.lineSeparator());

    return builder.toString();
  }
}
