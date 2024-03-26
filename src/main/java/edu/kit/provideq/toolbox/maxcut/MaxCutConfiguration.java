package edu.kit.provideq.toolbox.maxcut;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.maxcut.solvers.CirqMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.QiskitMaxCutSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the MaxCut graph problem.
 */
@Configuration
public class MaxCutConfiguration {
  /**
   * An optimization problem:
   * For a given graph, find the optimal separation of vertices that maximises the cut crossing edge
   * weight sum.
   */
  public static final ProblemType<String, String> MAX_CUT = new ProblemType<>(
      "max-cut",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getMaxCutManager(
      QiskitMaxCutSolver qiskitSolver,
      GamsMaxCutSolver gamsSolver,
      CirqMaxCutSolver cirqSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        MAX_CUT,
        Set.of(qiskitSolver, gamsSolver, cirqSolver),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("3-nodes-3-edges.txt"),
          "3-nodes-3-edges example for MaxCut is unavailable!"
      );
      var problem = new Problem<>(MAX_CUT);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
