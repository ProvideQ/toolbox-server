package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.featuremodel.SolveFeatureModelRequest;
import edu.kit.provideq.toolbox.meta.ProblemType;
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
   *      "Explaining Anomalies in Feature Models", Kowal et al., 2026</a>
   */
  public static final ProblemType<String, String> FEATURE_MODEL_ANOMALY_VOID = new ProblemType<>(
      "feature-model-anomaly-void",
      String.class,
      String.class,
      SolveFeatureModelRequest.class
  );
}
