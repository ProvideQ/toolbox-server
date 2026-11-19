package edu.kit.provideq.formats.cnf.dimacs;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import java.util.StringJoiner;

/**
 * Converts a DIMACS CNF formula into an equivalent logical expression string.
 *
 * <p>Clauses are joined with {@code and}, literals within a clause with {@code or};
 * negated variables are prefixed with {@code not}. The resulting expression is
 * logically equivalent to the CNF formula.
 */
public final class CnfToLogicalExpressionConverter {
  /**
   * Converter instance between DIMACS CNF and logical expressions.
   */
  public static final FuncConverter<Document<DimacsCnf>, String> INSTANCE =
      new FuncConverter<>(DimacsCnfFormat.INSTANCE, LogicalExpressionFormat.INSTANCE,
          CnfToLogicalExpressionConverter::convert);

  private CnfToLogicalExpressionConverter() {
  }

  private static String convert(Document<DimacsCnf> document) throws ParseException {
    var cnf = document.parse();
    var formula = new StringJoiner(" and ");
    for (var clause : cnf.getOrClauses()) {
      var literals = new StringJoiner(" or ");
      for (var variable : clause) {
        literals.add(literalToString(cnf, variable));
      }
      formula.add("(" + literals + ")");
    }
    return formula.toString();
  }

  private static String literalToString(DimacsCnf cnf, Variable variable) {
    var name = variable.name() != null ? variable.name() : String.valueOf(variable.number());

    return variable.isNegated() ? "not " + name : name;
  }
}
