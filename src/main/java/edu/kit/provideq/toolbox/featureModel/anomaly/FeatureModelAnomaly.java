package edu.kit.provideq.toolbox.featureModel.anomaly;

public enum FeatureModelAnomaly {
    /**
     * Occurs when no configuration of the feature model is ever valid
     */
    VOID,
    /**
     * Occurs when a feature is never true in any configuration
     */
    DEAD,
    /**
     * Occurs when a feature is flagged as optional, but in reality is mandatory and exists in all configurations
     */
    FALSE_OPTIONAL,
    /**
     * Constraints that don't have an impact on the valid configurations
     */
    REDUNDANT_CONSTRAINTS
}
