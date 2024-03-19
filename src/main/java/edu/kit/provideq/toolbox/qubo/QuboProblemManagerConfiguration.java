package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.toolbox.ProblemManager;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.TypedProblemType;
import edu.kit.provideq.toolbox.qubo.solvers.QiskitQuboSolver;
import edu.kit.provideq.toolbox.test.Problem;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuboProblemManagerConfiguration {
  /**
   * QUBO (Quadratic Unconstrained Binary Optimization)
   * A combinatorial optimization problem.
   * For a given quadratic term with binary decision variables,
   * find the minimal variable assignment of the term.
   */
  public static final TypedProblemType<String, String> QUBO =
      new TypedProblemType<>("qubo", String.class, String.class);
  @Bean
  ProblemManager<String, String> quboProblemManager(
      QiskitQuboSolver qiskitSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        QUBO,
        List.of(qiskitSolver),
        this.getExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> getExampleProblems(ResourceProvider resourceProvider) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("linear-problem.txt"),
          "linear-problem example for QUBO is unavailable!"
      );
      var problem = new Problem<>(QUBO);
      problem.setInput(resourceProvider.readStream(problemStream));
      return Set.of(problem);
    } catch (Exception e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
