package edu.kit.provideq.toolbox.featureModel.convert;

import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.io.FormatHandler;
import de.ovgu.featureide.fm.core.io.Problem;
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormatCNF;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;
import edu.kit.provideq.toolbox.exception.ConversionException;

import java.util.List;
import java.util.stream.Collectors;

public class UvlToDimacsCNF {
    static {
        FMCoreLibrary.getInstance().install();
    }

    public static String convert(String content) throws ConversionException {
        CharSequence charSequence = content.subSequence(0, content.length());

        // Init empty model
        var model = new MultiFeatureModel("de.ovgu.featureide.fm.core.MultiFeatureModelFactory");

        // Parse uvl
        var uvlFeatureModelHandler = new FormatHandler<>(new UVLFeatureModelFormat(), model);
        List<Problem> problems = uvlFeatureModelHandler.read(charSequence);
        if (problems.size() > 0) {
            throw new ConversionException("UVL conversion resulted in errors "
                    + problems
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n")));
        }

        IFeatureModel fm = uvlFeatureModelHandler.getObject();

        FeatureModelFormula featureModelFormula = new FeatureModelFormula(fm);
        var cnf = featureModelFormula.getCNF();

//        TseitinConverter tseitinConverter = new TseitinConverter();
//        var nodes = new ArrayList<Node>();
//        nodes.add(featureModelFormula.getCNFNode());
//        var convertedFM = tseitinConverter.convert(fm, nodes, false);
//        System.out.println(new DIMACSFormatCNF().write(new FeatureModelFormula(convertedFM).getCNF()));

        return new DIMACSFormatCNF().write(cnf);
    }
}
