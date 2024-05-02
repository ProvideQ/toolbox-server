package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model.tree;

import de.vill.model.Feature;
import de.vill.model.Group;
import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.image.SVGVector;


import java.util.ArrayList;
import java.util.List;

public class FeatureNode implements IFeatureNode {

    private final String name;
    private final FeatureNode parent;
    private final List<FeatureNode> children;
    private final Group.GroupType groupType;
    private final String ID;
    private double width;
    private double height;
    private SVGVector position;

    /**
     * Creates the root node and recursively creates all children from the given
     * root feature of the de.vill.model.FeatureModel
     *
     * @param feature root feature of the de.vill.model.FeatureModel
     * @param parent parent node of this node, should be null if this is the root node
     */

    public FeatureNode(FeatureNode parent, Feature feature) {
        this.parent = parent;
        this.name = feature.getFeatureName();
        this.children = new ArrayList<>();
        this.position = new SVGVector(2);
        this.ID = feature.getFullReference();
        if (this.isRoot()) {
            this.groupType = null;
        } else {
            this.groupType = feature.getParentGroup().GROUPTYPE;
        }
        createChildrenRecursivelyFromFeature(feature);
    }

    private boolean isRoot() {
        return parent == null;
    }

    private void createChildrenRecursivelyFromFeature(Feature feature) {
        for (Group group : feature.getChildren()) {
            for (Feature childFeature : group.getFeatures()) {
                addChild(new FeatureNode(this, childFeature));
            }
        }
    }

    public void addChild(FeatureNode child) {
        children.add(child);
    }

    public double getWidth() {
        return width;
    }

    @Override
    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public void setHeight(double height) {
        this.height = height;
    }

    @Override
    public double getX() {
        return position.getX();
    }

    @Override
    public void setX(double x) {
        this.position.setX(x);
    }

    @Override
    public double getY() {
        return position.getY();
    }

    @Override
    public void setY(double y) {
        this.position.setY(y);
    }

    @Override
    public double getCenterX() {
        return position.getX() + width / 2;
    }

    @Override
    public double getCenterY() {
        return position.getY() + height / 2;
    }

    public SVGVector getPosition() {
        return position;
    }

    @Override
    public SVGVector getEdgeOutPosition() {
        return new SVGVector(getCenterX(), getY() + getHeight());
    }

    @Override
    public SVGVector getEdgeInPosition() {
        return new SVGVector(getCenterX(), getY());
    }

    public String getName() {
        return name;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public Group.GroupType getGroupType() {
        return groupType;
    }

    public FeatureNode getParent() {
        return parent;
    }

    public List<FeatureNode> getChildren() {
        return children;
    }

    public String getTooltip() {
        String tooltip = "" + ID;
        if (groupType != null) {
            tooltip = groupType.name() + " " + tooltip;
        }
        return tooltip;
    }

}
