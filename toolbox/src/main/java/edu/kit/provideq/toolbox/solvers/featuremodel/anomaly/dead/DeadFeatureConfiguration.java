package edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.uvl.formats.UvlFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.formats.uvl.FeatureModelFamily;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead.formats.DeadFeatureSolutionFamily;
import edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead.solvers.SatBasedDeadFeatureSolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the "dead feature" feature model anomaly problem.
 */
@Configuration
public class DeadFeatureConfiguration {
  /**
   * A searching problem:
   * For a given feature model, check if the model contains dead features.
   *
   * @see <a href="https://sdq.kastel.kit.edu/publications/pdfs/kowal2016b.pdf">
   * "Explaining Anomalies in Feature Models", Kowal et al., 2016</a>
   */
  public static final ProblemType<FeatureModelFamily, DeadFeatureSolutionFamily>
      FEATURE_MODEL_ANOMALY_DEAD =
      new ProblemType<>(
      "FeatureModelAnomalyDead",
      "A searching problem: For a given feature model, check if the model contains dead features.",
      FeatureModelFamily.INSTANCE,
      DeadFeatureSolutionFamily.INSTANCE
  );

  @Bean
  DeadFeatureSolutionFamily registerDeadFeatureSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(DeadFeatureSolutionFamily.INSTANCE);
    return DeadFeatureSolutionFamily.INSTANCE;
  }

  @Bean
  ProblemManager<FeatureModelFamily, DeadFeatureSolutionFamily> getDeadFeatureManager(
      SatBasedDeadFeatureSolver satSolver,
      ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(
        FEATURE_MODEL_ANOMALY_DEAD, loadExampleProblems(resourceProvider));
    manager.registerSolvers(satSolver);
    return manager;
  }

  private Set<Problem<FeatureModelFamily, DeadFeatureSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("sandwich.txt"),
          "Sandwich example for Dead Feature is unavailable!"
      );
      var problem = new Problem<>(
          FEATURE_MODEL_ANOMALY_DEAD);
      problem.setInput(FormattedValue.of(
          resourceProvider.readStream(problemInputStream), UvlFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(FEATURE_MODEL_ANOMALY_DEAD, e);
    }
  }
}