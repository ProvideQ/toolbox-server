package edu.kit.provideq.formats.cnf.dimacs;

import edu.kit.provideq.formats.core.FormatException;
import edu.kit.provideq.formats.core.FuncConverter;
import edu.kit.provideq.formats.core.ParseException;

/**
 * Converts a logical expression string (e.g. "a and b or not c") into a DIMACS CNF formula.
 */
public final class ExpressionToDimacsCnfConverter {
  /**
   * Converter instance between logical expressions and DIMACS CNF.
   */
  public static final FuncConverter<String, DimacsCnf> INSTANCE =
      new FuncConverter<>(ExpressionFormat.INSTANCE, DimacsCnfFormat.INSTANCE, true,
          ExpressionToDimacsCnfConverter::convert);

  private ExpressionToDimacsCnfConverter() {
  }

  private static DimacsCnf convert(String expression) throws FormatException {
    try {
      return DimacsCnf.fromLogicalExpressionString(expression);
    } catch (ParseException e) {
      throw new FormatException("Could not convert logical expression to DIMACS CNF", e);
    }
  }
}
