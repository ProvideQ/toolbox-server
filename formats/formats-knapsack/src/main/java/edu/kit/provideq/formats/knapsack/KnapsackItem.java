package edu.kit.provideq.formats.knapsack;

/**
 * An item of a knapsack problem.
 *
 * @param id     identifier of the item
 * @param value  value of the item
 * @param weight weight of the item
 */
public record KnapsackItem(int id, int value, int weight) {
}
