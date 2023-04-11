package edu.kit.provideq.toolbox.featureModel.anomaly;

public enum FeatureModelAnomaly {
    /**
     * Occurs when no configuration of the feature model is ever valid
     */
    VOID {
        @Override
        public String toString() {
            return "Void Feature Model";
        }
    },
    /**
     * Occurs when a feature is never true in any configuration
     */
    DEAD {
        @Override
        public String toString() {
            return "Dead Features";
        }
    },
    /**
     * Occurs when a feature is flagged as optional, but in reality is mandatory and exists in all configurations
     */
    FALSE_OPTIONAL {
        @Override
        public String toString() {
            return "False-optional Features";
        }
    },
    /**
     * Constraints that don't have an impact on the valid configurations
     */
    REDUNDANT_CONSTRAINTS {
        @Override
        public String toString() {
            return "Redundant Constraints";
        }
    }
}
