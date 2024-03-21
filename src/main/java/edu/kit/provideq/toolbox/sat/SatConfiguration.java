package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sat.solvers.GamsSatSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
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

  @Bean
  ProblemManager<String, DimacsCnfSolution> getSatManager(
      GamsSatSolver gamsSolver,
      ResourceProvider resourceProvider,
      SubRoutinePool subRoutinePool
  ) {
    return new ProblemManager<>(
        SAT,
        Set.of(gamsSolver),
        loadExampleProblems(resourceProvider, subRoutinePool)
    );
  }

  private Set<Problem<String, DimacsCnfSolution>> loadExampleProblems(
      ResourceProvider resourceProvider,
      SubRoutinePool subRoutinePool
  ) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("simple-and.txt"),
          "Simple-And example for SAT is unavailable!"
      );
      var problem = new Problem<>(SAT, subRoutinePool);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
