package edu.kit.provideq.toolbox.solvers.qubo;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.lp.LPFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.qubo.solvers.DwaveQuboSolver;
import edu.kit.provideq.toolbox.solvers.qubo.solvers.GamsQuboSolver;
import edu.kit.provideq.toolbox.solvers.qubo.solvers.QiskitQuboSolver;
import edu.kit.provideq.toolbox.solvers.qubo.solvers.QrispQuboSolver;
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
  public static final ProblemType<QuboFamily, QuboSolutionFamily> QUBO =
      new ProblemType<>(
      "QUBO",
      "QUBO (Quadratic Unconstrained Binary Optimization) A combinatorial optimization problem. "
          + "For a given quadratic term with binary decision variables, find the "
          + "minimal variable assignment of the term.",
      QuboFamily.INSTANCE,
      QuboSolutionFamily.INSTANCE
  );

  @Bean
  QuboFamily registerQuboFamily(ConversionRegistry registry) {
    registry.registerFormat(QuboFamily.INSTANCE);
    return QuboFamily.INSTANCE;
  }

  @Bean
  QuboSolutionFamily registerQuboSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(QuboSolutionFamily.INSTANCE);
    return QuboSolutionFamily.INSTANCE;
  }

  @Bean
  LpSolutionToJsonQuboConverter registerLpSolutionToJsonQuboConverter(
      ConversionRegistry registry) {
    registry.registerConverter(LpSolutionToJsonQuboConverter.INSTANCE);
    return LpSolutionToJsonQuboConverter.INSTANCE;
  }

  @Bean
  LpSolutionToPositionalBitsConverter registerLpSolutionToPositionalBitsConverter(
      ConversionRegistry registry) {
    registry.registerContextConverter(LpSolutionToPositionalBitsConverter.INSTANCE);
    return LpSolutionToPositionalBitsConverter.INSTANCE;
  }

  @Bean
  PositionalBitsToLpSolutionConverter registerPositionalBitsToLpSolutionConverter(
      ConversionRegistry registry) {
    registry.registerContextConverter(PositionalBitsToLpSolutionConverter.INSTANCE);
    return PositionalBitsToLpSolutionConverter.INSTANCE;
  }

  @Bean
  JsonQuboToLpSolutionConverter registerJsonQuboToLpSolutionConverter(
      ConversionRegistry registry) {
    registry.registerConverter(JsonQuboToLpSolutionConverter.INSTANCE);
    return JsonQuboToLpSolutionConverter.INSTANCE;
  }

  @Bean
  ProblemManager<QuboFamily, QuboSolutionFamily> getQuboManager(
      QiskitQuboSolver qiskitSolver,
      DwaveQuboSolver dwaveSolver,
      QrispQuboSolver qrispSolver,
      GamsQuboSolver gamsQuboSolver,
      ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(QUBO, loadExampleProblems(resourceProvider));
    manager.registerSolvers(qiskitSolver, dwaveSolver, qrispSolver, gamsQuboSolver);
    return manager;
  }

  private Set<Problem<QuboFamily, QuboSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("qubo.lp"),
          "quadratic-problem example for QUBO is unavailable!"
      );
      var problem = new Problem<>(QUBO);
      problem.setInput(FormattedValue.of(

          resourceProvider.readStream(problemInputStream), LPFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(QUBO, e);
    }
  }
}
