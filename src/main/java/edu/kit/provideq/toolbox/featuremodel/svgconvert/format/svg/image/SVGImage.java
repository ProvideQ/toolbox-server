package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.image;

import de.vill.model.Group;
import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.image.definitions.SVGDefinitions;
import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model.tree.FeatureNode;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.commons.lang3.NotImplementedException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.geom.Point2D;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SVGImage {
    private static final String SVG_NAME_SPACE = SVGDOMImplementation.SVG_NAMESPACE_URI;
    private static final DOMImplementation DOM_IMPLEMENTATION = SVGDOMImplementation.getDOMImplementation();
    private final Element svgRoot;
    private final Map<String, Boolean> legendFlags;
    public double imageWidth;
    private final double imageHeight;
    private Document document;

    public SVGImage(double width, double height) {
        this.imageWidth = width;
        this.imageHeight = height;
        this.document = DOM_IMPLEMENTATION.createDocument(SVG_NAME_SPACE, "svg", null);
        this.document = new SVGDOMImplementation().createDocument(SVG_NAME_SPACE, "svg", null);
        this.svgRoot = document.getDocumentElement();
        this.legendFlags = new LinkedHashMap<>();
        initLegendFlags();
        showFullLegend();
        SVGDefinitions svgDefinitions = new SVGDefinitions(this, legendFlags);
        addToDocument(svgDefinitions.createDefinitions());
        drawLegend();
    }

    //Should be Job of ImageBuilder
    private void initLegendFlags() {
        legendFlags.put("mandatory", false);
        legendFlags.put("optional", false);
        legendFlags.put("or", false);
        legendFlags.put("alternative", false);
        legendFlags.put("abstract", false);
        legendFlags.put("concrete", false);
    }

    /**
     * Draws the legend in the middle right of the image
     * The legend is a rectangle with the legend elements inside
     * The legend elements are defined in the definitions section of the svg
     * The legend elements are defined in the SVGDefinitions class
     */

    public void drawLegend() {
        //The on the right side of the image
        //The hashmap is used to keep track of which legend elements are displayed
        // if the value is true the element is displayed
        double offset = 50;
        double x = imageWidth + offset;
        double y = 50;
        double width = 150;
        double height = 150;
        imageWidth += width + offset;

        var legend = createElement("use");
        legend.setAttribute("xlink:href", "#legend");
        legend.setAttribute("x", String.valueOf(x));
        legend.setAttribute("y", String.valueOf(y));
        drawLegendRectangle(x, y, width, height);
        addToDocument(legend);
    }

    private void drawLegendRectangle(double x, double y, double width, double height) {
        Element legendRectangle = createRectElement(x, y, width, height);
        legendRectangle.setAttribute("fill", "none");
        legendRectangle.setAttribute("stroke", "#666666");
        addToDocument(legendRectangle);
    }

    private void showFullLegend() {
        legendFlags.replaceAll((k, v) -> true);
    }

    private double getImageCenterX() {
        return imageWidth / 2;
    }

    public Element createElement(String name) {
        return document.createElementNS(SVG_NAME_SPACE, name);
    }

    private Element createRectElement(double x, double y, double width, double height) {
        var tag = createElement("rect");
        //drawLine(feature.calculateMiddlePointX(), feature.calculateMiddlePointY(), feature.calculateMiddlePointX(), 1000, "#111111");
        tag.setAttribute("x", String.valueOf(x));
        tag.setAttribute("y", String.valueOf(y));
        tag.setAttribute("width", String.valueOf(width));
        tag.setAttribute("height", String.valueOf(height));
        return tag;
    }

    private Element createFeatureLabel(String featureName, String featureId, double x, double centerY, double featureWidth) {
        Element featureText = createElement("text");

        Element path = createLabelCoordinates(x, centerY, featureWidth, featureId);
        Element textPath = createFeatureTextPathV2(featureName, featureId);

        featureText.appendChild(textPath);
        featureText.appendChild(path);
        return featureText;
    }

    public void addToDocument(Element element) {
        svgRoot.appendChild(element);
    }

    private Element createLabelCoordinates(double x, double centerY, double featureWidth, String featureId) {
        Element pathForText = createElement("path");
        pathForText.setAttribute("id", featureId);
        pathForText.setAttribute("d", "M" + x + "," + centerY + " h" + featureWidth);
        return pathForText;
    }

    private Element createFeatureTextPathV2(String featureText, String featureId) {
        Element textPath = createElement("textPath");
        textPath.setAttribute("xlink:href", "#" + featureId);
        textPath.setTextContent(sanitizeName(featureText));
        textPath.setAttribute("startOffset", "50%");
        return textPath;
    }

    public String sanitizeName (String name) {
        String result = name;
        result = result.replaceAll("", "");
        return result;
    }

    private Element createLine(double x1, double y1, double x2, double y2) {
        Element line = createElement("line");
        line.setAttribute("x1", String.valueOf(x1));
        line.setAttribute("y1", String.valueOf(y1));
        line.setAttribute("x2", String.valueOf(x2));
        line.setAttribute("y2", String.valueOf(y2));
        return line;
    }

    private Element createGroupArc(SVGVector startVector, SVGVector lineToVector, SVGVector endVector) {
        String arcCommand = getArcCommand(startVector, lineToVector, endVector);
        Element ellipse = createElement("path");
        ellipse.setAttribute("d", arcCommand);
        return ellipse;
    }

    private String getArcCommand(SVGVector startCoordinate, SVGVector lineToCoordinate, SVGVector endCoordinate) {
        final double arcYRadius = 0.6;
        final double arcXRadius = 1.0;
        String arcRadius = String.format("%s,%s", arcXRadius, arcYRadius);
        final String ARC_PATH_COMMAND_TEMPLATE = """
                M %s L %s A %s 0 0 0 %s Z
                """;
        return String.format(ARC_PATH_COMMAND_TEMPLATE, startCoordinate, lineToCoordinate, arcRadius, endCoordinate);
    }

    public String toString() {
        SVGGraphics2D svgGenerator = buildDocument();
        Writer out = new StringWriter();
        try {
            svgGenerator.stream(svgRoot, out, true, false);
        } catch (SVGGraphics2DIOException e) {
            e.printStackTrace();
        }
        return out.toString();
    }

    public SVGGraphics2D buildDocument() {
        svgRoot.setAttribute( "width", imageWidth + "px");
        svgRoot.setAttribute( "height", imageHeight + "px");

        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.create();

        return svgGenerator;
    }

    public void drawFeatureNodes(Set<FeatureNode> featureNodes) {
        for (FeatureNode featureNode : featureNodes) {
            drawFeatureNode(featureNode);
        }
    }

    private void drawFeatureNode(FeatureNode featureNode) {
        final double x = featureNode.getX();
        final double y = featureNode.getY();
        final double width = featureNode.getWidth();
        final double centerY = featureNode.getCenterY();
        final String featureNodeName = featureNode.getName();
        final String featureNodeId = featureNode.getID();

        var svgFeatureNode = createElement("g");
        Element tag = createRectElement(x, y, width, featureNode.getHeight());
        tag.setAttribute("class", "feature");
        svgFeatureNode.setAttribute("id", featureNode.getName());
        svgFeatureNode.appendChild(tag);
        Element featureToolTip = createElement("title");
        featureToolTip.setTextContent(featureNode.getTooltip());
        svgFeatureNode.appendChild(featureToolTip);
        var featureText = createFeatureLabel(featureNodeName, featureNodeId, x, centerY, width);
        svgFeatureNode.appendChild(featureText);
        addToDocument(svgFeatureNode);
    }



    public Element createLine(Point2D start, Point2D end) {
        return createLine(start.getX(), start.getY(), end.getX(), end.getY());
    }

    public void connectFeatures(Set<FeatureNode> featureNodes) {
        for (FeatureNode featureNode : featureNodes) {
            if (featureNode.getParent() == null) {
                recursivelyConnectFeatures(featureNode);
                break;
            }
        }
    }

    public void recursivelyConnectFeatures(FeatureNode parentNode) {
        for (FeatureNode child : parentNode.getChildren()) {
            connectFeatures(parentNode, child);
            recursivelyConnectFeatures(child);
        }
    }

    public void drawGroupArc(FeatureNode parentFeature, Group.GroupType grouptype) {
        //these vectors are used to calculate the points of the ellipse

        // Start point of the ellipse
        SVGVector startVector = parentFeature.getEdgeOutPosition();
        // LineTo Vector Calculation (in the arc command in SVG is the L the lineTo command)
        var firstChild = parentFeature.getChildren().get(0);
        SVGVector firstChildVector = firstChild.getEdgeInPosition();
        SVGVector lineToVector = firstChildVector.calculateScaledDiffVector(startVector);

        var lastChild = parentFeature.getChildren().get(parentFeature.getChildren().size() - 1);
        SVGVector lastChildVector = lastChild.getEdgeInPosition();
        SVGVector endVector = lastChildVector.calculateScaledDiffVector(startVector);

        Element ellipse = createGroupArc(startVector, lineToVector, endVector);
        ellipse.setAttribute("class", String.valueOf(grouptype).toLowerCase());
        addToDocument(ellipse);
    }

    private void connectFeatures(FeatureNode parentNode, FeatureNode child) {
        assert parentNode != null;
        assert child != null;
        Group.GroupType grouptype = child.getGroupType();
        if (grouptype == null) {
            return;
        }

        String lineName = parentNode.getName() + " → " + child.getName();
        Element line = createLine(parentNode.getEdgeOutPosition(), child.getEdgeInPosition());
        line.setAttribute("id", lineName);
        line.setAttribute("class", "line " + grouptype.toString().toLowerCase());
        switch (grouptype) {
            case OPTIONAL:
            case MANDATORY:
                break;
            case OR:
            case ALTERNATIVE:
                assert parentNode.getChildren().size() > 1;
                drawGroupArc(parentNode, grouptype);
                break;
            default:
                throw new NotImplementedException("Group type %s is not implemented".formatted(grouptype));

        }
        addToDocument(line);
    }

    public Element createDescription(String featureDescription) {
        var desc = createElement("desc");
        desc.setTextContent(featureDescription);
        return desc;
    }

}
