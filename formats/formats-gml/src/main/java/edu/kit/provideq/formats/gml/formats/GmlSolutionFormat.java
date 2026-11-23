package edu.kit.provideq.formats.gml.formats;

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
 * Annotated GML solution format emitted by MaxCut solvers.
 *
 * <p>The representation is a GML graph document whose nodes carry solver-added attributes
 * such as {@code partition}. The format declares this structure so the data can be
 * addressed deterministically; attribute readers can be added as needed.
 *
 * <p>Example instance (2-node graph, node 1 in partition 1, node 2 in partition 2):
 * <pre>
 * graph [
 *   node [
 *     id 1
 *     label "1"
 *     partition 1
 *   ]
 *   node [
 *     id 2
 *     label "2"
 *     partition 2
 *   ]
 *   edge [
 *     source 1
 *     target 2
 *     weight 5
 *   ]
 * ]
 * </pre>
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record GmlSolutionFormat() implements Format<String> {
  /**
   * Singleton instance of the GML solution format.
   */
  public static final GmlSolutionFormat INSTANCE = new GmlSolutionFormat();

  @Override
  public String getName() {
    return "gml-solution";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("gml");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<String> getRepresentationClass() {
    return String.class;
  }

  @Override
  public String parse(Reader reader) throws ParseException {
    try {
      return FormatReader.readAll(reader);
    } catch (IOException e) {
      throw new ParseException("Could not read GML solution input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize GML solution", e);
    }
  }
}