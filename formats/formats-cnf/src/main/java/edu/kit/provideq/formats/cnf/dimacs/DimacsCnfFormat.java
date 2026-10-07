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
 * The DIMACS CNF format for boolean formulas in conjunctive normal form.
 */
public record DimacsCnfFormat() implements Format<DimacsCnf> {
  /**
   * Singleton instance of the DIMACS CNF format.
   */
  public static final DimacsCnfFormat INSTANCE = new DimacsCnfFormat();

  @Override
  public String getName() {
    return "dimacs-cnf";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("cnf");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("cnf");
  }

  @Override
  public FormatKind getKind() {
    return FormatKind.PROBLEM;
  }

  @Override
  public DimacsCnf parse(Reader reader) throws ParseException {
    try {
      return DimacsCnf.fromDimacsCnfString(FormatReader.readAll(reader));
    } catch (IOException e) {
      throw new ParseException("Could not read DIMACS CNF input", e);
    }
  }

  @Override
  public void serialize(DimacsCnf value, Writer writer) throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize DIMACS CNF", e);
    }
  }
}
