package edu.kit.provideq.toolbox.tsp;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.tsplib.TSPLIBFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.tsp.solvers.LkhTspSolver;
import edu.kit.provideq.toolbox.tsp.solvers.QuboTspSolver;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TspConfiguration {

  /**
   * A Traveling Sales Person Problem.
   * Optimization Problem with the goal of find an optimal route
   *     between a given set of connected cities.
   */
  public static final ProblemType<TspFamily, TspSolutionFamily> TSP =
      new ProblemType<>(
      "TSP",
      "A Traveling Sales Person Problem. Optimization Problem with the goal of find "
      + "an optimal route between a given set of connected cities.",
      TspFamily.INSTANCE,
      TspSolutionFamily.INSTANCE
  );

  @Bean
  TspFamily registerTspFamily(ConversionRegistry registry) {
    registry.registerFormat(TspFamily.INSTANCE);
    return TspFamily.INSTANCE;
  }

  @Bean
  TspSolutionFamily registerTspSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(TspSolutionFamily.INSTANCE);
    return TspSolutionFamily.INSTANCE;
  }

  @Bean
  ProblemManager<TspFamily, TspSolutionFamily> getTspManager(
      ResourceProvider provider,
      QuboTspSolver quboTspSolver,
      LkhTspSolver lkhTspSolver
  ) {
    var manager = new ProblemManager<>(TSP, loadExampleProblems(provider));
    manager.registerSolvers(quboTspSolver, lkhTspSolver);
    return manager;
  }

  private Set<Problem<TspFamily, TspSolutionFamily>> loadExampleProblems(
      ResourceProvider provider) {
    try {
      String[] problemNames = new String[] {
          "att48.tsp", "SmallSampleTSP.tsp", "VerySmallSampleTSP.tsp"
      };

      var problemSet = new HashSet<Problem<TspFamily, TspSolutionFamily>>();
      for (var problemName : problemNames) {
        var problemStream = Objects.requireNonNull(getClass().getResourceAsStream(problemName),
            "Problem " + problemName + " not found");
        var problem = new Problem<TspFamily, TspSolutionFamily>(TSP);
        problem.setInput(FormattedValue.of(

            provider.readStream(problemStream), TSPLIBFormat.INSTANCE));
        problemSet.add(problem);
      }
      return problemSet;
    } catch (IOException e) {
      throw new MissingExampleException(TSP, e);
    }
  }
}
