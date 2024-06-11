package edu.kit.provideq.toolbox.knapsack;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * Definition and registration of the Knapsack problem.
 */
@Configuration
public class KnapsackConfiguration {
    /**
     * An optimization problem:
     * For given items each with a weight and value, determine which items are part of a collection where
     * the total weight is less than or equal to a given limit and the sum of values is as large as possible.
     */
    public static final ProblemType<String, String> KNAPSACK = new ProblemType<>(
            "knapsack",
            String.class,
            String.class
    );

    @Bean
    ProblemManager<String, String> getKnapsackManager(
            ResourceProvider resourceProvider
    ) {
        return new ProblemManager<>(
                KNAPSACK,
                Set.of(),
                loadExampleProblems(resourceProvider)
        );
    }

    private Set<Problem<String, String>> loadExampleProblems(
            ResourceProvider resourceProvider
    ) {
        try {
          var problemInputStream = Objects.requireNonNull(
                  getClass().getResourceAsStream("5-items.txt"),
                  "5-items example for Knapsack is unavailable!"
          );
          var problem = new Problem<>(KNAPSACK);
          problem.setInput(resourceProvider.readStream(problemInputStream));
          return Set.of(problem);
        } catch (IOException e) {
            throw new RuntimeException("Could not load example problems", e);
        }
    }

}
