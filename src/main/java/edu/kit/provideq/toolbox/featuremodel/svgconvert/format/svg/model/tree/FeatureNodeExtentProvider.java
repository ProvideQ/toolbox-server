package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model.tree;

import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.config.Configuration;
import org.abego.treelayout.NodeExtentProvider;


public class FeatureNodeExtentProvider implements NodeExtentProvider<FeatureNode> {
    public final double nodeHeight;
    public final double textToBorderMargin;
    public final double characterWidth;

    public FeatureNodeExtentProvider(double nodeHeight, double textToBorderMargin, double characterWidth) {
        this.nodeHeight = nodeHeight;
        this.textToBorderMargin = textToBorderMargin;
        this.characterWidth = characterWidth;
    }

    public FeatureNodeExtentProvider() {
        this.nodeHeight = Configuration.DEFAULT_HEIGHT;
        this.textToBorderMargin = Configuration.DEFAULT_TEXT_TO_BORDER_DISTANCE;
        this.characterWidth = Configuration.DEFAULT_WIDTH_PER_CHARACTER;
    }

    @Override
    public double getWidth(FeatureNode featureNode) {
        return textToBorderMargin + (featureNode.getName().length() * characterWidth) + textToBorderMargin;
    }

    @Override
    public double getHeight(FeatureNode featureNode) {
        return nodeHeight;
    }
}
