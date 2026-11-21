package edu.kit.provideq.toolbox.maxcut;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import edu.kit.provideq.formats.core.families.ContextBoundFormat;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

/**
 * The QAOA MaxCut solver report format emitted by the Qiskit MaxCut script.
 *
 * <p>The representation is a plain-text report with one {@code key: value} line per metric.
 * Example instance:
 * <pre>
 * energy:-1.8571428571428572
 * time:0.4310429096221924
 * max-cut objective:-1.8571428571428572
 * solution:[1. 0. 1.]
 * solution objective:8.0
 * </pre>
 *
 * <p>The {@code solution:} line carries the bitstring as a bracketed numpy array
 * ({@code [1. 0. 1.]} or {@code [1 0 1]} depending on the qiskit version); its bits assign
 * partitions to the nodes of the problem graph in node order.
 *
 * <p>The format is a context-bound member of the {@code MaxCut-Solution} family: converting
 * a report into a {@code gml-solution} requires the problem graph as context (the bits
 * assign partitions by node order), so the conversion is performed by the context-bound
 * {@link QaoaReportToGmlSolutionConverter} instead of a registry converter.
 */
public final class QaoaMaxCutReportFormat implements Format<String>, ContextBoundFormat {
  /**
   * Singleton instance of the QAOA MaxCut report format.
   */
  public static final QaoaMaxCutReportFormat INSTANCE = new QaoaMaxCutReportFormat();

  /**
   * Prefix of the line carrying the solution bitstring.
   */
  public static final String SOLUTION_LINE_PREFIX = "solution:";

  private QaoaMaxCutReportFormat() {
  }

  @Override
  public String getName() {
    return "qaoa-maxcut-report";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("qaoa-report");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<?> getRepresentationClass() {
    return String.class;
  }

  @Override
  public String parse(Reader reader) throws ParseException {
    try {
      var text = FormatReader.readAll(reader);
      if (text.lines().noneMatch(line -> line.startsWith(SOLUTION_LINE_PREFIX))) {
        throw new ParseException(
            "QAOA MaxCut report must contain a '%s' line".formatted(SOLUTION_LINE_PREFIX));
      }
      return text;
    } catch (IOException e) {
      throw new ParseException("Could not read QAOA MaxCut report", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize QAOA MaxCut report", e);
    }
  }
}
