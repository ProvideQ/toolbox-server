package edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.uvl.FeatureModelFamily;
import edu.kit.provideq.formats.uvl.formats.UvlFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.voidmodel.formats.VoidFeatureSolutionFamily;
import edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.voidmodel.solvers.SatBasedVoidFeatureSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the "void model" feature model anomaly problem.
 */
@Configuration
public class VoidModelConfiguration {
  /**
   * A searching problem:
   * For a given feature model, check if the model is void.
   *
   * @see <a href="https://sdq.kastel.kit.edu/publications/pdfs/kowal2016b.pdf">
   *      "Explaining Anomalies in Feature Models", Kowal et al., 2016</a>
   */
  public static final ProblemType<FeatureModelFamily, VoidFeatureSolutionFamily>
      FEATURE_MODEL_ANOMALY_VOID =
      new ProblemType<>(
      "FeatureModelAnomalyVoid",
      "A searching problem: For a given feature model, check if the model is void.",
      FeatureModelFamily.INSTANCE,
      VoidFeatureSolutionFamily.INSTANCE
  );

  @Bean
  VoidFeatureSolutionFamily registerVoidFeatureSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(VoidFeatureSolutionFamily.INSTANCE);
    return VoidFeatureSolutionFamily.INSTANCE;
  }

  @Bean
  ProblemManager<FeatureModelFamily, VoidFeatureSolutionFamily> getVoidModelManager(
      SatBasedVoidFeatureSolver satSolver,
      ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(
        FEATURE_MODEL_ANOMALY_VOID, loadExampleProblems(resourceProvider));
    manager.registerSolvers(satSolver);
    return manager;
  }

  private Set<Problem<FeatureModelFamily, VoidFeatureSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("sandwich.txt"),
          "Sandwich example for Void Feature Models is unavailable!"
      );
      var problem = new Problem<>(FEATURE_MODEL_ANOMALY_VOID);
      var input = resourceProvider.readStream(problemInputStream);
      problem.setInput(FormattedValue.of(input, UvlFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(FEATURE_MODEL_ANOMALY_VOID, e);
    }
  }
}
