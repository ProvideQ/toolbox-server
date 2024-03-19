package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

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
public class DeadFeatureManagerConfiguration {
  /**
   * A searching problem:
   * For a given feature model, check if the model contains dead features.
   *
   * @see <a href="https://sdq.kastel.kit.edu/publications/pdfs/kowal2016b.pdf">
   *      "Explaining Anomalies in Feature Models", Kowal et al., 2026</a>
   */
  public static final TypedProblemType<String, String> FEATURE_MODEL_ANOMALY_DEAD =
      new TypedProblemType<>("feature-model-anomaly-dead", String.class, String.class);

  @Bean
  ProblemManager<String, String> problemManager(
      SatBasedDeadFeatureSolver sat,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        FEATURE_MODEL_ANOMALY_DEAD,
        List.of(sat),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("sandwich.txt"),
          "Sandwich example for Dead Feature is unavailable!"
      );
      var problem = new Problem<>(FEATURE_MODEL_ANOMALY_DEAD);
      problem.setInput(resourceProvider.readStream(problemStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
