package edu.kit.provideq.toolbox.solvers.maxcut;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.gml.Gml;
import edu.kit.provideq.formats.gml.GmlFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.solvers.maxcut.solvers.CirqMaxCutSolver;
import edu.kit.provideq.toolbox.solvers.maxcut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.solvers.maxcut.solvers.QiskitMaxCutSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the MaxCut graph problem.
 */
@Configuration
public class MaxCutConfiguration {
  /**
   * An optimization problem:
   * For a given graph, find the optimal separation of vertices that maximizes the cut crossing edge
   * weight sum.
   */
  public static final ProblemType<MaxCutFamily, MaxCutSolutionFamily> MAX_CUT =
      new ProblemType<>(
      "MaxCut",
      "An optimization problem: For a given graph, find the optimal separation of vertices "
          + "that maximises the cut crossing edge weight sum.",
      MaxCutFamily.INSTANCE,
      MaxCutSolutionFamily.INSTANCE
  );

  @Bean
  MaxCutFamily registerMaxCutFamily(ConversionRegistry registry) {
    registry.registerFormat(MaxCutFamily.INSTANCE);
    return MaxCutFamily.INSTANCE;
  }

  @Bean
  MaxCutSolutionFamily registerMaxCutSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(MaxCutSolutionFamily.INSTANCE);
    registry.registerFormat(QaoaMaxCutReportFormat.INSTANCE);
    registry.registerContextConverter(QaoaReportToGmlSolutionConverter.INSTANCE);
    registry.registerConverter(GmlSolutionToQaoaReportConverter.INSTANCE);
    return MaxCutSolutionFamily.INSTANCE;
  }

  @Bean
  ProblemManager<MaxCutFamily, MaxCutSolutionFamily> getMaxCutManager(
      QiskitMaxCutSolver qiskitSolver,
      GamsMaxCutSolver gamsSolver,
      CirqMaxCutSolver cirqSolver,
      ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(MAX_CUT, loadExampleProblems(resourceProvider));
    manager.registerSolvers(qiskitSolver, gamsSolver, cirqSolver);
    return manager;
  }

  private Set<Problem<MaxCutFamily, MaxCutSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("3-nodes-3-edges.txt"),
          "3-nodes-3-edges example for MaxCut is unavailable!"
      );
      var problem = new Problem<>(MAX_CUT);
      problem.setInput(FormattedValue.of(
          Document.of(resourceProvider.readStream(problemInputStream), Gml::fromString),
          GmlFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(MAX_CUT, e);
    }
  }
}