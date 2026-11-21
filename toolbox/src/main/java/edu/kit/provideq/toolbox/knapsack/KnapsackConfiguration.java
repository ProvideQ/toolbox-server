package edu.kit.provideq.toolbox.knapsack;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.knapsack.KnapsackFormat;
import edu.kit.provideq.formats.knapsack.KnapsackItem;
import edu.kit.provideq.formats.knapsack.KnapsackProblem;
import edu.kit.provideq.formats.knapsack.KnapsackSolution;
import edu.kit.provideq.toolbox.Bound;
import edu.kit.provideq.toolbox.BoundType;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.knapsack.solvers.PythonKnapsackSolver;
import edu.kit.provideq.toolbox.knapsack.solvers.QiskitKnapsackSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
  private static final Function<FormattedValue<?>, Bound> estimator = input -> {
    @SuppressWarnings("unchecked")
    var problemDocument = (Document<KnapsackProblem>) input.getValue();
    KnapsackProblem problem;
    try {
      problem = problemDocument.parse();
    } catch (ParseException e) {
      throw new ConversionException(
          "Could not read knapsack problem: " + e.getMessage(), e);
    }
    List<KnapsackItem> items = new ArrayList<>(problem.items());
    int capacity = problem.capacity();
    // sort items by descending value-to-weight density
    items.sort(Comparator.comparingDouble(item -> -item.value() / (double) item.weight()));
    // keep adding items until the weight limit is reached, then add a fraction of the next item
    float bound = 0;
    int currentWeight = 0;
    for (var item : items) {
      if (currentWeight + item.weight() <= capacity) {
        bound += item.value();
        currentWeight += item.weight();
      } else {
        var fraction = (capacity - currentWeight) / (float) item.weight();
        bound += (fraction * item.value());
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
  public static final ProblemType<KnapsackFamily, KnapsackSolutionFamily> KNAPSACK =
      new ProblemType<>(
      "Knapsack",
      "An optimization problem: For given items each with a weight and value, "
          + "determine which items are part of a collection where the total weight is "
          + "less than or equal to a given limit and the sum of values is as large as possible.",
      KnapsackFamily.INSTANCE,
      KnapsackSolutionFamily.INSTANCE,
      estimator,
      "^(\\d+)"
  );

  @Bean
  KnapsackFamily registerKnapsackFamily(ConversionRegistry registry) {
    registry.registerFormat(KnapsackFamily.INSTANCE);
    return KnapsackFamily.INSTANCE;
  }

  @Bean
  KnapsackSolutionFamily registerKnapsackSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(KnapsackSolutionFamily.INSTANCE);
    return KnapsackSolutionFamily.INSTANCE;
  }

  @Bean
  ProblemManager<KnapsackFamily, KnapsackSolutionFamily> getKnapsackManager(
          PythonKnapsackSolver pythonKnapsackSolver,
          QiskitKnapsackSolver qiskitKnapsackSolver,
          ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(KNAPSACK, loadExampleProblems(resourceProvider));
    manager.registerSolvers(pythonKnapsackSolver, qiskitKnapsackSolver);
    return manager;
  }

  private Set<Problem<KnapsackFamily, KnapsackSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("4-items.txt"),
          "4-items example for Knapsack is unavailable!"
      );
      var problem = new Problem<>(KNAPSACK);
      problem.setInput(FormattedValue.parse(
          resourceProvider.readStream(problemInputStream), KnapsackFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException | ParseException e) {
      throw new MissingExampleException(KNAPSACK, e);
    }
  }
}