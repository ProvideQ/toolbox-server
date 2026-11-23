package edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead;

import java.util.List;

/**
 * Structured result of dead-feature analysis.
 *
 * @param featureNames names of features that are dead in the analyzed feature model
 */
public record DeadFeatureSolution(List<String> featureNames) {
  public DeadFeatureSolution {
    featureNames = List.copyOf(featureNames);
  }
}
