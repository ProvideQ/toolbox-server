package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model.tree;

import org.abego.treelayout.util.AbstractTreeForTreeLayout;

import java.util.List;

public class FeatureNodeTree extends AbstractTreeForTreeLayout<FeatureNode> {

    public FeatureNodeTree(FeatureNode root) {
        super(root);
    }


    public FeatureNode getParent(FeatureNode featureNode) {
        return featureNode.getParent();
    }

    @Override
    public List<FeatureNode> getChildrenList(FeatureNode featureNode) {
        return (List<FeatureNode>) featureNode.getChildren();
    }
}
