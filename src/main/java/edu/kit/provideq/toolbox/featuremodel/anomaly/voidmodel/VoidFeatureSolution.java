package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Structured result of a void-feature-model analysis.
 *
 * @param isVoid               {@code true} if the feature model is void,
 *                             i.e. no valid configuration exists
 * @param exampleConfiguration a valid configuration as mapping from feature name to its state,
 *                             empty if the feature model is void
 */
public record VoidFeatureSolution(boolean isVoid,
                                  Supplier<Map<String, Boolean>> exampleConfiguration) {
}
