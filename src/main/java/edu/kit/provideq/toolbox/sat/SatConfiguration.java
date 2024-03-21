package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the satisfiability problem.
 */
@Configuration
public class SatConfiguration {
  /**
   * A satisfiability problem:
   * For a given boolean formula, check if there is an interpretation that satisfies the formula.
   */
  public static final ProblemType<String, DimacsCnfSolution> SAT = new ProblemType<>(
      "sat",
      String.class,
      DimacsCnfSolution.class,
      SolveSatRequest.class
  );
}
