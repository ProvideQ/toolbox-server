package edu.kit.provideq.toolbox.maxcut;

import edu.kit.provideq.toolbox.ProblemManager;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.maxcut.solvers.CirqMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.QiskitMaxCutSolver;
import edu.kit.provideq.toolbox.meta.TypedProblemType;
import edu.kit.provideq.toolbox.test.Problem;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaxCutConfiguration {
  /**
   * An optimization problem:
   * For a given graph, find the optimal separation of vertices that maximises the cut crossing edge
   * weight sum.
   */
  public static final TypedProblemType<String, String> MAX_CUT =
      new TypedProblemType<>("max-cut", String.class, String.class);

  @Bean
  ProblemManager<String, String> getProblemManager(
      QiskitMaxCutSolver qiskitSolver,
      GamsMaxCutSolver gamsSolver,
      CirqMaxCutSolver cirqSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        MAX_CUT,
        List.of(qiskitSolver, gamsSolver, cirqSolver),
        this.getExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> getExampleProblems(ResourceProvider resourceProvider) {
    try {
      var problemStream = Objects.requireNonNull(
          MaxCutConfiguration.class.getResourceAsStream("3-nodes-3-edges.txt"),
          "3-nodes-3-edges example for MaxCut is unavailable!"
      );
      var problem = new Problem<>(MAX_CUT);
      problem.setInput(resourceProvider.readStream(problemStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
