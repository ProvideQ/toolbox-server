package edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.image.definitions;

import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.image.SVGImage;
import org.w3c.dom.Element;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SVGDefinitions {
    public static final String MARKER_TYPE = "Circle";
    public static final String MANDATORY_MARKER_ID = "mandatory";
    public static final String OPTIONAL_MARKER_ID = "optional";



    private final SVGImage svgRoot;
    private final Map<String, Boolean> legendFlags;

    public SVGDefinitions(SVGImage svgRoot, Map<String, Boolean> legendFlags) {
        this.svgRoot = svgRoot;
        this.legendFlags = legendFlags;
    }

    //Needs to be replaced by loading a file
    public Element createDefinitions() {
        var defs = createElement("defs");
        List<Element> definitionElements = new ArrayList<>();
        definitionElements.add(createStyleTag());

        //Add Markers to definitions
        initializeMarkers(definitionElements);

        //Add Legend Items to definitions
        definitionElements.add(createLegendItemMandatory());
        definitionElements.add(createLegendItemOptional());
        definitionElements.add(createLegendItemAbstract());
        definitionElements.add(createLegendItemConcrete());
        definitionElements.add(createLegendItemOr());
        definitionElements.add(createLegendItemAlternative());
        //Add Legend to definitions
        definitionElements.add((createLegend()));

        definitionElements.forEach(defs::appendChild);
        return defs;
//        document.getDocumentElement().appendChild(defs);
    }

    private Element createElement(String elementName) {
        return svgRoot.createElement(elementName);
    }

    private Element createStyleTag() {
        var styleTag = createElement("style");
        String style = """
                
                    symbol { overflow: visible; }
                    marker { overflow: visible; }
                    rect.feature {
                        stroke: #9999bf;
                        fill:   #ccccff;
                    }
                    rect.abstractFeature {
                        stroke: #9999bf;
                        fill:   #f2f2ff;
                    }
                    rect.constraint {
                        stroke: #9999bf;
                        fill:   #ffffff;
                    }
                    circle.mandatory {
                        stroke: #666666;
                        fill:   #666666;
                    }
                    circle.optional {
                        stroke: #666666;
                        fill:   #ffffff;
                    }
                    line.mandatory {
                        stroke: #666666;
                        fill:   #666666;
                        marker-end: url(#mandatory);
                    }
                    line.optional {
                        stroke: #666666;
                        fill:   #ffffff;
                        marker-end: url(#optional);
                    }
                    path.or {
                        stroke: #666666;
                        fill:   #666666;
                    }
                    path.alternative {
                        stroke: #666666;
                        fill:   none;
                    }
                    line {
                        stroke: #666666;
                        stroke-width: 1;
                    }
                    textPath {
                        font-family: Arial;
                        font-size: 12px;
                        text-anchor: middle;
                        dominant-baseline: central;
                    }
                    * {
                        font-family: Arial;
                        font-size: 12px;
                    }""";
        styleTag.setTextContent(style);

//        System.out.println(styleTag.toString());
        return styleTag;
    }

    private void initializeMarkers(List<Element> definitionElements) {
        // Config that is needed to create Markers:
        // id, titleText, classType,
        definitionElements.add(createCircleSymbol("mandatoryCircle", "mandatory", "Mandatory"));
        definitionElements.add(createCircleSymbol("optionalCircle", "optional", "Optional"));
        definitionElements.add(createMarker(MANDATORY_MARKER_ID, "#mandatoryCircle"));
        definitionElements.add(createMarker(OPTIONAL_MARKER_ID, "#optionalCircle"));
    }

    private Element createLegendItemMandatory() {
        var mandatoryLegendItem = createElement("symbol");
        mandatoryLegendItem.setAttribute("id", "mandatoryLegendItem");
        var circle = createElement("use");
        circle.setAttribute("xlink:href", "#mandatoryCircle");
        mandatoryLegendItem.appendChild(circle);
        var line = createElement("line");
        line.setAttribute("x1", "0");
        line.setAttribute("y1", "0");
        line.setAttribute("x2", "10");
        line.setAttribute("y2", "-10");
        mandatoryLegendItem.appendChild(line);
        var text = createElement("text");
        text.setAttribute("x", "20");
        text.setAttribute("y", "2");
        text.setTextContent("Mandatory");
        mandatoryLegendItem.appendChild(text);
        var title = createElement("title");
        title.setTextContent("Mandatory");
        mandatoryLegendItem.appendChild(title);
        return mandatoryLegendItem;
    }

    private Element createLegendItemOptional() {
        var optionalLegendItem = createElement("symbol");
        optionalLegendItem.setAttribute("id", "optionalLegendItem");
        var line = createElement("line");
        line.setAttribute("x1", "0");
        line.setAttribute("y1", "0");
        line.setAttribute("x2", "10");
        line.setAttribute("y2", "-10");
        optionalLegendItem.appendChild(line);
        var circle = createElement("use");
        circle.setAttribute("xlink:href", "#optionalCircle");
        optionalLegendItem.appendChild(circle);
        var text = createElement("text");
        text.setAttribute("x", "20");
        text.setAttribute("y", "2");
        text.setTextContent("Optional");
        optionalLegendItem.appendChild(text);
        var title = createElement("title");
        title.setTextContent("Optional");
        optionalLegendItem.appendChild(title);
        return optionalLegendItem;
    }

    private Element createLegendItemAbstract() {
        var abstractLegendItem = createElement("symbol");
        abstractLegendItem.setAttribute("id", "abstractLegendItem");
        var rect = createElement("rect");
        rect.setAttribute("x", "-8");
        rect.setAttribute("y", "-10");
        rect.setAttribute("class", "abstractFeature");
        rect.setAttribute("width", "20");
        rect.setAttribute("height", "12");
        abstractLegendItem.appendChild(rect);
        var text = createElement("text");
        text.setAttribute("x", "20");
        text.setAttribute("y", "0");
        text.setTextContent("Abstract Feature");
        abstractLegendItem.appendChild(text);
        var title = createElement("title");
        title.setTextContent("Abstract Feature");
        abstractLegendItem.appendChild(title);
        return abstractLegendItem;
    }

    private Element createLegendItemConcrete() {
        var concreteLegendItem = createElement("symbol");
        concreteLegendItem.setAttribute("id", "concreteLegendItem");
        var line = createElement("line");
        line.setAttribute("x1", "0");
        line.setAttribute("y1", "0");
        line.setAttribute("x2", "10");
        line.setAttribute("y2", "-10");
        concreteLegendItem.appendChild(line);
        var rect = createElement("rect");
        rect.setAttribute("x", "-8");
        rect.setAttribute("y", "-10");
        rect.setAttribute("class", "feature");
        rect.setAttribute("width", "20");
        rect.setAttribute("height", "12");
        concreteLegendItem.appendChild(rect);
        var text = createElement("text");
        text.setAttribute("x", "20");
        text.setAttribute("y", "0");
        text.setTextContent("Concrete Feature");
        concreteLegendItem.appendChild(text);
        var title = createElement("title");
        title.setTextContent("Concrete Feature");
        concreteLegendItem.appendChild(title);
        return concreteLegendItem;
    }

    private Element createLegendItemOr() {
        var orLegendItem = createElement("symbol");
        orLegendItem.setAttribute("id", "orLegendItem");
        var path1 = createElement("path");
        path1.setAttribute("d", "M 0,0 L 5,-10 10,0");
        path1.setAttribute("stroke", "#666666");
        path1.setAttribute("fill", "none");
        orLegendItem.appendChild(path1);
        var path2 = createElement("path");
        path2.setAttribute("d", "M 2.5,-5 A 1,1 185 0 0 7.5,-5 L 5,-10");
        path2.setAttribute("fill", "#666666");
        orLegendItem.appendChild(path2);
        var text = createElement("text");
        text.setAttribute("x", "20");
        text.setAttribute("y", "0");
        text.setTextContent("Or Group");
        orLegendItem.appendChild(text);
        var title = createElement("title");
        title.setTextContent("Or Group");
        orLegendItem.appendChild(title);
        return orLegendItem;
    }

    private Element createLegendItemAlternative() {
        var alternativeLegendItem = createElement("symbol");
        alternativeLegendItem.setAttribute("id", "alternativeLegendItem");
        var path1 = createElement("path");
        path1.setAttribute("d", "M 0,0 L 5,-10 10,0");
        path1.setAttribute("stroke", "#666666");
        path1.setAttribute("fill", "none");
        alternativeLegendItem.appendChild(path1);
        var path2 = createElement("path");
        path2.setAttribute("d", "M 2.5,-5 A 1,1 185 0 0 7.5,-5 L 5,-10");
        path2.setAttribute("fill", "none");
        path2.setAttribute("stroke", "#666666");
        alternativeLegendItem.appendChild(path2);
        var text = createElement("text");
        text.setAttribute("x", "20");
        text.setAttribute("y", "0");
        text.setTextContent("Alternative Group");
        alternativeLegendItem.appendChild(text);
        var title = createElement("title");
        title.setTextContent("Alternative Group");
        alternativeLegendItem.appendChild(title);
        return alternativeLegendItem;
    }

    private Element createLegend() {
        String legendId = "legend";
        String legendTextX = "20";
        String legendTextY = "10";
        var legend = createElement("symbol");
        legend.setAttribute("id", legendId);
        var text = createElement("text");
        text.setAttribute("x", legendTextX);
        text.setAttribute("y", legendTextY);
        text.setTextContent("Legend");
        legend.appendChild(text);
        var title = createElement("title");
        title.setTextContent("Legend");
        legend.appendChild(title);

        double legendItemStartX = 10;
        double legendItemStartY = 40;
        double legendItemDeltaY = 20;

        for (Map.Entry<String, Boolean> mapEntry : legendFlags.entrySet()) {
            if (mapEntry.getValue()) {
                var legendItem = createElement("use");
                legendItem.setAttribute("xlink:href", "#%sLegendItem".formatted(mapEntry.getKey()));
                legendItem.setAttribute("x", String.valueOf(legendItemStartX));
                legendItem.setAttribute("y", String.valueOf(legendItemStartY));
                legend.appendChild(legendItem);
                legendItemStartY += legendItemDeltaY;
            }
        }
        return legend;
    }

    private Element createCircleSymbol(String id, String classType, String titleText) {
        var mandatoryCircle = createElement("symbol");
        mandatoryCircle.setAttribute("id", id);
        var circle = createElement("circle");
        circle.setAttribute("class", classType);
        circle.setAttribute("r", "3.5");
        circle.setAttribute("stroke-width", "1");
        mandatoryCircle.appendChild(circle);
        var title = createElement("title");
        title.setTextContent(titleText);
        mandatoryCircle.appendChild(title);
        return mandatoryCircle;
    }

    private Element createMarker(String id, String link) {
        var mandatoryMarker = createElement("marker");
        mandatoryMarker.setAttribute("id", id);
        var use = createElement("use");
        use.setAttribute("xlink:href", link);
        mandatoryMarker.appendChild(use);
        return mandatoryMarker;
    }
}
