package edu.kit.provideq.formats.sat.formats.dimacs;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

/**
 * The DIMACS CNF solution format for assignments of boolean formulas.
 *
 * <p>The representation follows the DIMACS solution conventions: an optional
 * {@code s cnf <satisfiable> <variables> <clauses>} status line followed by one or more
 * {@code v} lines, each carrying signed literal numbers and terminated by {@code 0};
 * positive numbers assign true, negative numbers assign false.
 *
 * <p>Example instance assigning {@code a = true, b = true}:
 * <pre>
 * c 2 b
 * c 1 a
 * s cnf 1 2 2
 * v 2
 * v 1
 * </pre>
 *
 * <p>The solution format needs the corresponding CNF formula to map variable numbers to
 * names; use {@link #withFormula(DimacsCnf)} to create a formula-bound view of this format.
 */
public record DimacsCnfSolutionFormat(DimacsCnf formula)
    implements Format<Document<DimacsCnfSolution>> {
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
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<DimacsCnfSolution> getRepresentationClass() {
    return DimacsCnfSolution.class;
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
  public Document<DimacsCnfSolution> parse(Reader reader) throws ParseException {
    try {
      // The returned document retains the raw input; it is parsed into the structured
      // solution lazily on Document.parse().
      return Document.of(FormatReader.readAll(reader), this::parseSolution);
    } catch (IOException e) {
      throw new ParseException("Could not read DIMACS CNF solution input", e);
    }
  }

  private DimacsCnfSolution parseSolution(String solutionString) throws ParseException {
    if (formula == null) {
      throw new ParseException(
          "DIMACS CNF solution requires the corresponding CNF formula; "
              + "use withFormula(DimacsCnf)");
    }

    return DimacsCnfSolution.fromString(formula, solutionString);
  }

  @Override
  public void serialize(Document<DimacsCnfSolution> value, Writer writer)
      throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize DIMACS CNF solution", e);
    }
  }
}