package edu.kit.provideq.formats.sat.formats.dimacs;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * The DIMACS CNF format for boolean formulas in conjunctive normal form.
 *
 * <p>Example instance for the formula {@code (a or not b) and (not a or c)}:
 * <pre>
 * c This is a comment line
 * p cnf 3 2
 * 1 -2 0
 * -1 3 0
 * </pre>
 */
public record DimacsCnfFormat() implements Format<Document<DimacsCnf>> {
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
  public FormatType getType() {
    return FormatType.PROBLEM;
  }

  @Override
  public Class<DimacsCnf> getRepresentationClass() {
    return DimacsCnf.class;
  }

  @Override
  public Map<String, AttributeReader<Document<DimacsCnf>, ?>> getAttributeReaders() {
    return Map.of(
        "variable_count",
        AttributeReader.of(Integer.class, cnf -> cnf.parse().getVariables().size()),
        "clause_count",
        AttributeReader.of(Integer.class, cnf -> cnf.parse().getOrClauses().size()));
  }

  @Override
  public Document<DimacsCnf> parse(Reader reader) throws ParseException {
    try {
      // The returned document retains the raw input; it is parsed into the structured
      // CNF lazily on Document.parse().
      return Document.of(FormatReader.readAll(reader), DimacsCnf::fromDimacsCnfString);
    } catch (IOException e) {
      throw new ParseException("Could not read DIMACS CNF input", e);
    }
  }

  /**
   * Serializes the given DIMACS CNF object to the provided writer.
   * The format looks like this:
   * <pre>
   *   c This is a comment line
   *   p cnf 3 2
   *   1 -3 0
   *   2 3 -1 0
   * </pre>
   *
   * @param value object representation to serialize
   * @param writer target of the serialized output
   * @throws SerializationException if there is an error during serialization
   */
  @Override
  public void serialize(Document<DimacsCnf> value, Writer writer)
      throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize DIMACS CNF", e);
    }
  }
}