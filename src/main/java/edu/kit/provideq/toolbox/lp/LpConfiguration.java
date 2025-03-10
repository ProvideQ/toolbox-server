package edu.kit.provideq.toolbox.lp;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.lp.solvers.OrToolsMipCbc;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the "Quadratic Unconstrained Binary Optimization" problem.
 */
@Configuration
public class LpConfiguration {
  /**
   * QUBO (Quadratic Unconstrained Binary Optimization)
   * A combinatorial optimization problem.
   * For a given quadratic term with binary decision variables,
   * find the minimal variable assignment of the term.
   */
  public static final ProblemType<String, String> LP = new ProblemType<>(
      "lp",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getLpManager(
      OrToolsMipCbc orToolsMipCbc,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        LP,
        Set.of(orToolsMipCbc),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("qubo.lp"),
          "quadratic-problem example for QUBO is unavailable!"
      );
      var problem = new Problem<>(LP);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(LP, e);
    }
  }
}
