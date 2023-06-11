package edu.kit.provideq.toolbox.featureModel.anomaly;

public enum FeatureModelAnomaly {
    /**
     * Occurs when no configuration of the feature model is ever valid
     */
    VOID("Void Feature Model"),
    /**
     * Occurs when a feature is never true in any configuration
     */
    DEAD("Dead Features"),
    /**
     * Occurs when a feature is flagged as optional, but in reality is mandatory and exists in all configurations
     */
    FALSE_OPTIONAL("False-optional Features"),
    /**
     * Constraints that don't have an impact on the valid configurations
     */
    REDUNDANT_CONSTRAINTS("Redundant Constraints");

    public final String name;

    FeatureModelAnomaly(String name) {
        this.name = name;
    }
}
