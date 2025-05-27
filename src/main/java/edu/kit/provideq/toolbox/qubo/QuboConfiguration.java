package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.qubo.solvers.DwaveQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.KipuQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QiskitQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QrispQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QuantagoniaQuboSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the "Quadratic Unconstrained Binary Optimization" problem.
 */
@Configuration
public class QuboConfiguration {
  /**
   * QUBO (Quadratic Unconstrained Binary Optimization)
   * A combinatorial optimization problem.
   * For a given quadratic term with binary decision variables,
   * find the minimal variable assignment of the term.
   */
  public static final ProblemType<String, String> QUBO = new ProblemType<>(
      "qubo",
      "QUBO (Quadratic Unconstrained Binary Optimization) A combinatorial optimization problem. "
          + "For a given quadratic term with binary decision variables, find the "
          + "minimal variable assignment of the term.",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getQuboManager(
      QiskitQuboSolver qiskitSolver,
      DwaveQuboSolver dwaveSolver,
      QrispQuboSolver qrispSolver,
      QuantagoniaQuboSolver quantagoniaQuboSolver,
      KipuQuboSolver kipuQuboSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        QUBO,
        Set.of(qiskitSolver, dwaveSolver, qrispSolver, quantagoniaQuboSolver, kipuQuboSolver),
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
      var problem = new Problem<>(QUBO);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(QUBO, e);
    }
  }
}
