package edu.kit.provideq.toolbox.solvers.sat;

import edu.kit.provideq.formats.cnf.dimacs.LogicalExpressionFormat;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.sat.solvers.GamsSatSolver;
import edu.kit.provideq.toolbox.solvers.sat.solvers.QrispExactGroverSolver;
import edu.kit.provideq.toolbox.solvers.sat.solvers.QrispGroverSolver;
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
  public static final ProblemType<SatFamily, SatSolutionFamily> SAT =
      new ProblemType<>(
          "SAT",
          "A satisfiability problem: For a given boolean formula, check "
              + "if there is an interpretation that satisfies the formula.",
          SatFamily.INSTANCE,
          SatSolutionFamily.INSTANCE
      );

  /**
   * Registers the SAT format families with the shared registry.
   *
   * @param registry the shared conversion registry
   * @return the registry with the SAT families registered
   */
  @Bean
  SatFamily registerSatFamily(ConversionRegistry registry) {
    registry.registerFormat(SatFamily.INSTANCE);
    return SatFamily.INSTANCE;
  }

  @Bean
  SatSolutionFamily registerSatSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(SatSolutionFamily.INSTANCE);
    return SatSolutionFamily.INSTANCE;
  }

  @Bean
  ProblemManager<SatFamily, SatSolutionFamily> getSatManager(
      GamsSatSolver gamsSolver,
      QrispGroverSolver qrispSolver,
      QrispExactGroverSolver exactSolver,
      ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(SAT, loadExampleProblems(resourceProvider));
    manager.registerSolvers(gamsSolver, qrispSolver, exactSolver);
    return manager;
  }

  private Set<Problem<SatFamily, SatSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("simple-and.txt"),
          "Simple-And example for SAT is unavailable!"
      );
      var problem = new Problem<>(SAT);
      problem.setInput(FormattedValue.of(
          resourceProvider.readStream(problemInputStream),
          LogicalExpressionFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(SAT, e);
    }
  }
}