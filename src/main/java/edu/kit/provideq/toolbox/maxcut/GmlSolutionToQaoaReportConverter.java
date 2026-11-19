package edu.kit.provideq.toolbox.maxcut;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.Converter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.gml.Gml;
import edu.kit.provideq.formats.gml.GmlFormat;
import edu.kit.provideq.formats.gml.GmlSolutionFormat;
import java.io.StringReader;

/**
 * Converts a {@link GmlSolutionFormat GML solution} into a
 * {@link QaoaMaxCutReportFormat QAOA MaxCut report} containing the solution bitstring.
 *
 * <p>The conversion reads the node partitions from the solution graph in node order and
 * maps them back to the QAOA bit convention (partition 1 to bit 0, partition 2 to bit 1).
 * The report metrics of the original solver run (energy, time, objective values) cannot
 * be recovered from a partition assignment, so the report contains only the
 * {@code solution:} line, which is the part the format's parser requires.
 *
 * <p>Example conversion:
 * <pre>
 * input:                       output:
 * graph [                      solution:[0. 1.]
 *   node [ id 1  partition 1 ]
 *   node [ id 2  partition 2 ]
 * ]
 * </pre>
 */
public final class GmlSolutionToQaoaReportConverter implements Converter<String, String> {
  /**
   * Singleton instance of the converter.
   */
  public static final GmlSolutionToQaoaReportConverter INSTANCE =
      new GmlSolutionToQaoaReportConverter();

  private GmlSolutionToQaoaReportConverter() {
  }

  @Override
  public Format<String> getSource() {
    return GmlSolutionFormat.INSTANCE;
  }

  @Override
  public Format<String> getTarget() {
    return QaoaMaxCutReportFormat.INSTANCE;
  }

  @Override
  public String convert(String gmlSolution) throws FormatException {
    Gml solutionGraph;
    try {
      solutionGraph = GmlFormat.INSTANCE.parse(new StringReader(gmlSolution)).parse();
    } catch (ParseException e) {
      throw new FormatException("Could not parse GML solution: " + e.getMessage());
    }

    var bits = new StringBuilder("[");
    for (var node : solutionGraph.nodes()) {
      var partition = node.attributes().get("partition");
      if (partition == null) {
        throw new FormatException(
            "GML solution node %s carries no partition attribute".formatted(node.id()));
      }
      var bit = switch (partition) {
        case "1" -> "0.";
        case "2" -> "1.";
        default -> throw new FormatException(
            "GML solution node %s has unexpected partition %s".formatted(node.id(), partition));
      };
      if (bits.length() > 1) {
        bits.append(' ');
      }
      bits.append(bit);
    }
    bits.append(']');

    return QaoaMaxCutReportFormat.SOLUTION_LINE_PREFIX + bits;
  }
}
