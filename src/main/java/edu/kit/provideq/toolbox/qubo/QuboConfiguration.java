package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.toolbox.meta.ProblemType;
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
}
