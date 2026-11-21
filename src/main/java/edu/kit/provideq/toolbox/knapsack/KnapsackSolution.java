package edu.kit.provideq.toolbox.knapsack;

import java.util.List;

/**
 * Object representation of a knapsack solution.
 *
 * @param totalValue      the total value of the selected items
 * @param selectedItemIds the identifiers of the selected items
 */
public record KnapsackSolution(int totalValue, List<Integer> selectedItemIds) {
  public KnapsackSolution {
    selectedItemIds = List.copyOf(selectedItemIds);
  }

  /**
   * Returns the solution in the knapsack solution format notation: the total value followed
   * by one line with the identifier of each selected item.
   */
  @Override
  public String toString() {
    var builder = new StringBuilder();
    builder.append(totalValue).append(System.lineSeparator());
    for (var id : selectedItemIds) {
      builder.append(id).append(System.lineSeparator());
    }

    return builder.toString();
  }
}
