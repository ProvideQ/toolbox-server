package edu.kit.provideq.formats.cnf.dimacs;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatKind;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.ParseException;
import edu.kit.provideq.formats.core.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

/**
 * The DIMACS CNF solution format for assignments of boolean formulas.
 *
 * <p>The solution format needs the corresponding CNF formula to map variable numbers to
 * names; use {@link #withFormula(DimacsCnf)} to create a formula-bound view of this format.
 */
public record DimacsCnfSolutionFormat(DimacsCnf formula) implements Format<DimacsCnfSolution> {
  /**
   * Base instance of the DIMACS CNF solution format without a bound formula.
   */
  public static final DimacsCnfSolutionFormat INSTANCE = new DimacsCnfSolutionFormat(null);

  @Override
  public String getName() {
    return "dimacs-cnf-solution";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of();
  }

  @Override
  public FormatKind getKind() {
    return FormatKind.SOLUTION;
  }

  /**
   * Returns a format instance bound to the given CNF formula,
   * so that parsing solutions maps variable numbers to names.
   *
   * @param formula the CNF formula the solutions belong to
   * @return formula-bound format instance
   */
  public DimacsCnfSolutionFormat withFormula(DimacsCnf formula) {
    return new DimacsCnfSolutionFormat(formula);
  }

  @Override
  public DimacsCnfSolution parse(Reader reader) throws ParseException {
    try {
      var solutionString = FormatReader.readAll(reader);
      if (formula == null) {
        throw new ParseException(
            "DIMACS CNF solution requires the corresponding CNF formula; "
                + "use withFormula(DimacsCnf)");
      }

      return DimacsCnfSolution.fromString(formula, solutionString);
    } catch (IOException e) {
      throw new ParseException("Could not read DIMACS CNF solution input", e);
    }
  }

  @Override
  public void serialize(DimacsCnfSolution value, Writer writer) throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize DIMACS CNF solution", e);
    }
  }
}
