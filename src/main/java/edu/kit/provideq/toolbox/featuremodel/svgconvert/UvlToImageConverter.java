package edu.kit.provideq.toolbox.featuremodel.svgconvert;

import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.image.SVGImage;
import edu.kit.provideq.toolbox.featuremodel.svgconvert.format.svg.model.SVGImageBuilder;
import org.apache.batik.transcoder.svg2svg.PrettyPrinter;

import java.io.StringReader;
import java.io.StringWriter;


/**
 * Converts a UVL file to an image
 * supported formats: SVG, PNG, PDF, JPEG
 * for Png, Pdf and Jpeg, the SVG file is first generated and then converted
 */
public class UvlToImageConverter {
    private final SVGImage svgImage;

    public UvlToImageConverter(String uvl) {
        svgImage = preprocessToSvgFormat(uvl);
    }

    private SVGImage preprocessToSvgFormat(String uvl) {
        SVGImageBuilder svgImageBuilder = new SVGImageBuilder(uvl);
        return svgImageBuilder.getSvgImage();
    }

    public String visualize() {
        return svgImage.toString();
    }

    /**
     *
     * @param uvl representation of a feature model
     * @return a String containing the svg representation of the feature model
     */
    public static String visualize(String uvl) {
        UvlToImageConverter converter = new UvlToImageConverter(uvl);
        String svg = converter.visualize();
        String svgForWeb = converter.postProcessSVGForWeb(svg);
        return svgForWeb;

    }

    public String postProcessSVGForWeb(String svgString) {
        PrettyPrinter pp = new PrettyPrinter();
        pp.setDoctypeOption(PrettyPrinter.DOCTYPE_REMOVE);
        pp.setXMLDeclaration("");
        pp.setNewline(System.lineSeparator());
        pp.setTabulationWidth(4);

        StringReader reader = new StringReader(svgString);
        StringWriter writer = new StringWriter();

        try {
            pp.print(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Gebe den verarbeiteten SVG-String zurück
        return writer.toString();
    }

}




