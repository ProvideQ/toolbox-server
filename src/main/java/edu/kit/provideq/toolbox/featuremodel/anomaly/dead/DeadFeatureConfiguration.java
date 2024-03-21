package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

import edu.kit.provideq.toolbox.featuremodel.SolveFeatureModelRequest;
import edu.kit.provideq.toolbox.meta.ProblemType;
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
   *      "Explaining Anomalies in Feature Models", Kowal et al., 2026</a>
   */
  public static final ProblemType<String, String> FEATURE_MODEL_ANOMALY_DEAD = new ProblemType<>(
      "feature-model-anomaly-dead",
      String.class,
      String.class,
      SolveFeatureModelRequest.class
  );
}
