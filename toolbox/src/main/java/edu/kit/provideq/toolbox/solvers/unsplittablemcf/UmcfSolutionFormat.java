package edu.kit.provideq.toolbox.solvers.unsplittablemcf;

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
 * The unsplittable multi commodity flow result report format.
 *
 * <p>The representation is the result report emitted by the GAMSPy solver backends
 * (an HTML report document). The report structure is declared so the data can be
 * addressed deterministically; attribute readers can be added as needed.
 *
 * <p>Example instance:
 * <pre>
 * &lt;html&gt;
 *   &lt;body&gt;
 *     &lt;h1&gt;Unsplittable MCF Solution&lt;/h1&gt;
 *     &lt;!-- routing visualization of the solved instance --&gt;
 *   &lt;/body&gt;
 * &lt;/html&gt;
 * </pre>
 */
public record UmcfSolutionFormat() implements Format<String> {
  /**
   * Singleton instance of the UMCF solution format.
   */
  public static final UmcfSolutionFormat INSTANCE = new UmcfSolutionFormat();

  @Override
  public String getName() {
    return "umcf-result-report";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("html");
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
      throw new ParseException("Could not read UMCF result report", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize UMCF result report", e);
    }
  }
}