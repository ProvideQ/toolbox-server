package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model;

import de.vill.main.UVLModelFactory;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.image.SVGImage;
import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model.tree.*;
import org.abego.treelayout.TreeLayout;

import java.awt.geom.Rectangle2D;
import java.util.Map;

/**
 * Should create the SVG file from uvl text
 */
public class SVGImageBuilder {

    private final String uvlText;
    private final FeatureModel featureModel;
    private final SVGImage svgImage;
    private FeatureNode root;

    public SVGImageBuilder(String uvlText) {
        this.uvlText = uvlText;
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = uvlModelFactory.parse(uvlText);
        this.featureModel = featureModel;
        this.svgImage = build(featureModel);
    }

    public SVGImage build(FeatureModel featureModel) {
        TreeLayout<FeatureNode> treeLayout = initializeTreeLayout(featureModel);
        initializeNodeCoordinates(treeLayout);

        double height = calculateTreeHeight(treeLayout);
        double width = calculateTreeWidth(treeLayout);
        SVGImage svgImage = new SVGImage(width, height);
        svgImage.drawFeatureNodes(treeLayout.getNodeBounds().keySet());
        svgImage.connectFeatures(treeLayout.getNodeBounds().keySet());
        return svgImage;
    }

    private TreeLayout<FeatureNode> initializeTreeLayout(FeatureModel featureModel) {
        Feature rootFeature = featureModel.getRootFeature();
        FeatureNode root = new FeatureNode(null, rootFeature);
        this.root = root;
        FeatureIDETreeLayoutConfiguration configuration = new FeatureIDETreeLayoutConfiguration();
        FeatureNodeTree featureTree = new FeatureNodeTree(root);
        FeatureNodeExtentProvider extentProvider = new FeatureNodeExtentProvider();
        TreeLayout<FeatureNode> treeLayout = new TreeLayout<FeatureNode>(featureTree, extentProvider, configuration);
        return treeLayout;
    }

    private void initializeNodeCoordinates(TreeLayout<FeatureNode> treeLayout) {
        System.out.println(treeLayout.getNodeBounds());
        var bounds = treeLayout.getNodeBounds();
        for (Map.Entry<FeatureNode, Rectangle2D.Double> entry : bounds.entrySet()) {
            IFeatureNode featureNode = entry.getKey();
            Rectangle2D.Double rectangle = entry.getValue();
            updateFeatureNodeImageInformation(featureNode, rectangle);
        }
    }

    private double calculateTreeHeight(TreeLayout<FeatureNode> treeLayout) {
        //this height constant is arbitrary chosen
        final int heightConstant = 200;
        return treeLayout.getLevelCount() * heightConstant;
    }

    private double calculateTreeWidth(TreeLayout<FeatureNode> treeLayout) {
        var bounds = treeLayout.getNodeBounds();
        var entries = bounds.entrySet();
        //get the first element as a sample
        double result = entries.iterator().next().getValue().x;
        for (Map.Entry<FeatureNode, Rectangle2D.Double> entry : entries) {
            result = Math.max(result, entry.getValue().x + entry.getValue().width);
        }
        return result;
    }

    private void updateFeatureNodeImageInformation(IFeatureNode featureNode, Rectangle2D.Double rectangle) {
        featureNode.setX(rectangle.getX());
        featureNode.setY(rectangle.getY());
        featureNode.setWidth(rectangle.getWidth());
        featureNode.setHeight(rectangle.getHeight());
    }

    public SVGImage getSvgImage() {
        return svgImage;
    }
}
