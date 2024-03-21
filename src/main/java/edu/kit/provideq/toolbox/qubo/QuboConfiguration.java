package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.qubo.solvers.QiskitQuboSolver;
import edu.kit.provideq.toolbox.sat.solvers.GamsSatSolver;
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
      String.class,
      String.class,
      SolveQuboRequest.class
  );

  @Bean
  ProblemManager<String, String> getQuboManager(
      QiskitQuboSolver qiskitSolver,
      ResourceProvider resourceProvider,
      SubRoutinePool subRoutinePool
  ) {
    return new ProblemManager<>(
        QUBO,
        Set.of(qiskitSolver),
        loadExampleProblems(resourceProvider, subRoutinePool)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider,
      SubRoutinePool subRoutinePool
  ) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("linear-problem.txt"),
          "linear-problem example for QUBO is unavailable!"
      );
      var problem = new Problem<>(QUBO, subRoutinePool);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
