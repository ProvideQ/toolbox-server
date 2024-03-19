package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.ProblemManager;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.TypedProblemType;
import edu.kit.provideq.toolbox.sat.solvers.GamsSatSolver;
import edu.kit.provideq.toolbox.test.Problem;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SatManagerConfiguration {
  /**
   * A satisfiability problem:
   * For a given boolean formula, check if there is an interpretation that satisfies the formula.
   */
  public static final TypedProblemType<String, DimacsCnfSolution> SAT =
      new TypedProblemType<>("sat", String.class, DimacsCnfSolution.class);

  @Bean
  ProblemManager<String, DimacsCnfSolution> getSatManager(
      GamsSatSolver gamsSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        SAT,
        Set.of(gamsSolver),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, DimacsCnfSolution>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("simple-and.txt"),
          "Simple-And example for SAT is unavailable!"
      );
      var problem = new Problem<>(SAT);
      problem.setInput(resourceProvider.readStream(problemStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
