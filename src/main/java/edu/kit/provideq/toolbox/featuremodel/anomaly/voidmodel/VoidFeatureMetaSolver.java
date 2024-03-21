package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This is the meta solver for the {@link VoidModelConfiguration#FEATURE_MODEL_ANOMALY_VOID}
 * problem.
 */
@Component
public class VoidFeatureMetaSolver
    extends MetaSolver<String, String, ProblemSolver<String, String>> {
  private final String examplesDirectoryPath;
  private final ResourceProvider resourceProvider;

  @Autowired
  public VoidFeatureMetaSolver(
          @Value("${examples.directory.feature-model}") String examplesDirectoryPath,
          ResourceProvider resourceProvider,
          SatBasedVoidFeatureSolver solver) {
    super(VoidModelConfiguration.FEATURE_MODEL_ANOMALY_VOID, solver);
    this.examplesDirectoryPath = examplesDirectoryPath;
    this.resourceProvider = resourceProvider;
  }

  @Override
  public List<String> getExampleProblems() {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("sandwich.txt"),
          "Sandwich example for Void Feature Models is unavailable!"
      );
      return List.of(resourceProvider.readStream(problemStream));
    } catch (IOException e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
