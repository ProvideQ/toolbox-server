package edu.kit.provideq.toolbox.knapsack;

import edu.kit.provideq.toolbox.Bound;
import edu.kit.provideq.toolbox.BoundType;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.knapsack.solvers.PythonKnapsackSolver;
import edu.kit.provideq.toolbox.knapsack.solvers.QiskitKnapsackSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the Knapsack problem.
 */
@Configuration
public class KnapsackConfiguration {
  /**
   * an upper bound estimator by solving the greedy fractional knapsack problem.
   */
  private static final Function<String, Bound> estimator = input -> {
    var parts = input.split("\n");
    var weightLimit = Integer.parseInt(parts[parts.length - 1]);
    // map item value to item weight
    List<Map.Entry<Integer, Integer>> items = new ArrayList<>();
    for (int i = 1; i < parts.length - 1; i++) {
      var item = parts[i].split(" ");
      items.add(new AbstractMap.SimpleEntry<>(
              Integer.parseInt(item[1]),
              Integer.parseInt(item[2]))
      );
    }
    items.sort(Comparator.comparingInt(a -> -a.getKey() / a.getValue()));
    // keep adding items until the weight limit is reached, then add a fraction of the next item
    float bound = 0;
    int currentWeight = 0;
    while (currentWeight < weightLimit) {
      var item = items.remove(0);
      var value = item.getKey();
      var weight = item.getValue();
      if (currentWeight + weight <= weightLimit) {
        bound += value;
        currentWeight += weight;
      } else {
        var fraction = (weightLimit - currentWeight) / (float) weight;
        bound += (fraction * value);
        break;
      }
    }
    return new Bound(bound, BoundType.UPPER);
  };

  /**
   * An optimization problem:
   * For given items each with a weight and value, determine which items are part of a collection
   * where the total weight is less than or equal to a given limit
   * and the sum of values is as large as possible.
   */
  public static final ProblemType<String, String> KNAPSACK = new ProblemType<>(
      "Knapsack",
      "An optimization problem: For given items each with a weight and value, "
          + "determine which items are part of a collection where the total weight is "
          + "less than or equal to a given limit and the sum of values is as large as possible.",
      String.class,
      String.class,
      estimator,
      "^(\\d+)",
      Map.ofEntries(
          Map.entry("item_count", problem -> problem.lines()
              .findFirst()
              .orElse("")),
          Map.entry("capacity", p2 -> p2.lines()
              .reduce((a, b) -> b)
              .orElse(""))
      )
  );

  @Bean
  ProblemManager<String, String> getKnapsackManager(
          PythonKnapsackSolver pythonKnapsackSolver,
          QiskitKnapsackSolver qiskitKnapsackSolver,
          ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
            KNAPSACK,
            Set.of(pythonKnapsackSolver, qiskitKnapsackSolver),
            loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("4-items.txt"),
          "4-items example for Knapsack is unavailable!"
      );
      var problem = new Problem<>(KNAPSACK);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(KNAPSACK, e);
    }
  }
}
