package edu.kit.provideq.toolbox.solvers.unsplittable_mcf;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.formats.UmcfFamily;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.formats.UmcfFormat;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.formats.UmcfSolutionFamily;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.solvers.GamsUnsplittableMcfClassicalSolver;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.solvers.GamsUnsplittableMcfQuboSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the Unsplittable Multi Commodity Flow problem.
 */
@Configuration
public class UnsplittableMcfConfiguration {

  /**
   * An optimization problem:
   * For a given time-expanded network with products, orders, and capacity constraints,
   * find optimal routing paths to fulfill customer orders through a Time-Expanded Network
   * without splitting an order's demand.
   */
  public static final ProblemType<UmcfFamily, UmcfSolutionFamily> UNSPLITTABLE_MCF =
      new ProblemType<>(
      "UnsplittableMCF",
      "An optimization problem: For a given time-expanded network with products, orders, "
          + "and capacity constraints, find optimal routing paths to fulfill customer orders "
          + "through a Time-Expanded Network without splitting an order's demand",
      UmcfFamily.INSTANCE,
      UmcfSolutionFamily.INSTANCE
  );

  @Bean
  UmcfFamily registerUmcfFamily(ConversionRegistry registry) {
    registry.registerFormat(UmcfFamily.INSTANCE);
    return UmcfFamily.INSTANCE;
  }

  @Bean
  UmcfSolutionFamily registerUmcfSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(UmcfSolutionFamily.INSTANCE);
    return UmcfSolutionFamily.INSTANCE;
  }

  @Bean
  ProblemManager<UmcfFamily, UmcfSolutionFamily> getUnsplittableMcfManager(
      GamsUnsplittableMcfClassicalSolver classicalSolver,
      GamsUnsplittableMcfQuboSolver lpExportSolver,
      ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(UNSPLITTABLE_MCF, loadExampleProblems(resourceProvider));
    manager.registerSolvers(classicalSolver, lpExportSolver);
    return manager;
  }

  private Set<Problem<UmcfFamily, UmcfSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("unsplittable_instance.json"),
          "Example instance for UnsplittableMCF is unavailable!"
      );
      var problem = new Problem<>(UNSPLITTABLE_MCF);
      problem.setInput(FormattedValue.parse(
          resourceProvider.readStream(problemInputStream), UmcfFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException | ParseException e) {
      throw new MissingExampleException(UNSPLITTABLE_MCF, e);
    }
  }
}