package edu.kit.provideq.toolbox.solvers.sharpsat;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.sat.SatFamily;
import edu.kit.provideq.formats.sat.formats.logical.LogicalExpressionFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.sharpsat.formats.SolutionCountFamily;
import edu.kit.provideq.toolbox.solvers.sharpsat.solvers.GanakSolver;
import edu.kit.provideq.toolbox.solvers.sharpsat.solvers.PythonBruteForceSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the satisfiability solution counting problems.
 */
@Configuration
public class SharpSatConfiguration {
  /**
   * A satisfiability counting problem:
   * For a given boolean formula, count number of interpretations that satisfies the formula.
   * NP-Hard in nature.
   */
  public static final ProblemType<SatFamily, SolutionCountFamily> SHARPSAT =
      new ProblemType<>(
      "SharpSAT",
      "A satisfiability counting problem: For a given boolean formula, count number of "
          + "interpretations that satisfies the formula. NP-Hard in nature.",
      SatFamily.INSTANCE,
      SolutionCountFamily.INSTANCE
  );

  @Bean
  SolutionCountFamily registerSolutionCountFamily(ConversionRegistry registry) {
    registry.registerFormat(SolutionCountFamily.INSTANCE);
    return SolutionCountFamily.INSTANCE;
  }

  @Bean
  ProblemManager<SatFamily, SolutionCountFamily> getSharpSatManager(
      PythonBruteForceSolver pythonBruteForceSolver,
      GanakSolver ganakSolver,
      ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(SHARPSAT, loadExampleProblems(resourceProvider));
    manager.registerSolvers(pythonBruteForceSolver, ganakSolver);
    return manager;
  }

  private Set<Problem<SatFamily, SolutionCountFamily>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("simple-and.txt"),
          "Simple-And example for SAT is unavailable!"
      );
      var problem = new Problem<>(SHARPSAT);
      problem.setInput(FormattedValue.of(
          resourceProvider.readStream(problemInputStream), LogicalExpressionFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(SHARPSAT, e);
    }
  }
}
