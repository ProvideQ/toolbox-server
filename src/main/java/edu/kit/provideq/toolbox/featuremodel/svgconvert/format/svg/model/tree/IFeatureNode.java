package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model.tree;
import de.vill.model.Group;

import java.awt.geom.Point2D;
import java.util.List;

public interface IFeatureNode {
    double getWidth();

    void setWidth(double width);

    double getHeight();

    void setHeight(double height);

    double getX();

    void setX(double x);

    double getY();

    void setY(double y);

    double getCenterX();

    double getCenterY();

    Point2D getPosition();

    Point2D getEdgeOutPosition();

    Point2D getEdgeInPosition();

    String getName();

    String getID();

    Group.GroupType getGroupType();

    IFeatureNode getParent();

    List<? extends IFeatureNode> getChildren();
}
