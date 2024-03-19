package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.ProblemManager;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.TypedProblemType;
import edu.kit.provideq.toolbox.test.Problem;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VoidFeatureModelManagerConfiguration {
  /**
   * A searching problem:
   * For a given feature model, check if the model is void.
   *
   * @see <a href="https://sdq.kastel.kit.edu/publications/pdfs/kowal2016b.pdf">
   *      "Explaining Anomalies in Feature Models", Kowal et al., 2026</a>
   */
  public static final TypedProblemType<String, String> FEATURE_MODEL_ANOMALY_VOID =
      new TypedProblemType<>("feature-model-anomaly-void", String.class, String.class);

  @Bean
  ProblemManager<String, String> configureManager(
      SatBasedVoidFeatureSolver sat,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        FEATURE_MODEL_ANOMALY_VOID,
        List.of(sat),
        this.loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("sandwich.txt"),
          "Sandwich example for Void Feature Models is unavailable!"
      );
      var problem = new Problem<>(FEATURE_MODEL_ANOMALY_VOID);
      problem.setInput(resourceProvider.readStream(problemStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
