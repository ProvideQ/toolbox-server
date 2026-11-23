package edu.kit.provideq.formats.sat.converters;

import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnf;
import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnfFormat;
import edu.kit.provideq.formats.sat.formats.logical.LogicalExpressionFormat;
import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.exceptions.ParseException;

/**
 * Converts a logical expression string (e.g. "a and b or not c") into a DIMACS CNF formula.
 */
public final class LogicalExpressionToDimacsCnfConverter {
  /**
   * Converter instance between logical expressions and DIMACS CNF.
   */
  public static final FuncConverter<String, Document<DimacsCnf>> INSTANCE =
      new FuncConverter<>(LogicalExpressionFormat.INSTANCE, DimacsCnfFormat.INSTANCE,
          LogicalExpressionToDimacsCnfConverter::convert);

  private LogicalExpressionToDimacsCnfConverter() {
  }

  private static Document<DimacsCnf> convert(String expression) throws ParseException {
    return Document.of(DimacsCnf.fromLogicalExpressionString(expression));
  }
}
