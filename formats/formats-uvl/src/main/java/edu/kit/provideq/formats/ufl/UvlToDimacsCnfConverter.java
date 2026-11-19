package edu.kit.provideq.formats.ufl;

import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.io.FormatHandler;
import de.ovgu.featureide.fm.core.io.Problem;
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormatCNF;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfFormat;
import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts UVL feature models into DIMACS CNF formulas.
 */
public final class UvlToDimacsCnfConverter {
  static {
    FMCoreLibrary.getInstance().install();
  }

  /**
   * Converter instance between UVL feature models and DIMACS CNF.
   */
  public static final FuncConverter<String, Document<DimacsCnf>> INSTANCE =
      new FuncConverter<>(UvlFormat.INSTANCE, DimacsCnfFormat.INSTANCE,
          UvlToDimacsCnfConverter::convert);

  private UvlToDimacsCnfConverter() {
  }

  private static Document<DimacsCnf> convert(String value) throws FormatException {
    CharSequence charSequence = value.subSequence(0, value.length());

    // Init empty model
    var model = new MultiFeatureModel("de.ovgu.featureide.fm.core.MultiFeatureModelFactory");

    // Parse uvl
    var uvlFeatureModelHandler = new FormatHandler<>(new UVLFeatureModelFormat(), model);
    List<Problem> problems = uvlFeatureModelHandler.read(charSequence);
    if (!problems.isEmpty()) {
      throw new FormatException("UVL conversion resulted in errors "
          + problems
          .stream()
          .map(Object::toString)
          .collect(Collectors.joining("\n")));
    }

    IFeatureModel fm = uvlFeatureModelHandler.getObject();

    FeatureModelFormula featureModelFormula = new FeatureModelFormula(fm);
    var cnf = featureModelFormula.getCNF();

    var cnfString = new DIMACSFormatCNF().write(cnf);

    return Document.of(cnfString, DimacsCnf::fromDimacsCnfString);
  }
}