package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model.tree;

import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.config.Configuration;
import org.abego.treelayout.util.DefaultConfiguration;

public class FeatureIDETreeLayoutConfiguration extends DefaultConfiguration {
    private static final double GAP_BETWEEN_LEVELS = Configuration.DEFAULT_VERTICAL_DISTANCE_TO_PARENT;
    private static final double GAP_BETWEEN_NODES = Configuration.DEFAULT_TEXT_TO_BORDER_DISTANCE;

    public FeatureIDETreeLayoutConfiguration() {
        super(GAP_BETWEEN_LEVELS, GAP_BETWEEN_NODES);
    }


    public FeatureIDETreeLayoutConfiguration(double gapBetweenLevels, double gapBetweenNodes, Location location, AlignmentInLevel alignmentInLevel) {
        super(gapBetweenLevels, gapBetweenNodes, location, alignmentInLevel);
    }

    public FeatureIDETreeLayoutConfiguration(double gapBetweenLevels, double gapBetweenNodes, Location location) {
        super(gapBetweenLevels, gapBetweenNodes, location);
    }

    public FeatureIDETreeLayoutConfiguration(double gapBetweenLevels, double gapBetweenNodes) {
        super(gapBetweenLevels, gapBetweenNodes);
    }
}
