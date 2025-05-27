package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
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
  public static final ProblemType<String, String> FEATURE_MODEL_ANOMALY_DEAD = new ProblemType<>(
      "FeatureModelAnomalyDead",
      "A searching problem: For a given feature model, check if the model contains dead features.",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getDeadFeatureManager(
      SatBasedDeadFeatureSolver satSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        FEATURE_MODEL_ANOMALY_DEAD,
        Set.of(satSolver),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("sandwich.txt"),
          "Sandwich example for Dead Feature is unavailable!"
      );
      var problem = new Problem<>(FEATURE_MODEL_ANOMALY_DEAD);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(FEATURE_MODEL_ANOMALY_DEAD, e);
    }
  }
}
