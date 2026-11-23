package edu.kit.provideq.toolbox.solvers.maxcut;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ContextConverter;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.gml.Gml;
import edu.kit.provideq.formats.gml.GmlSolutionFormat;
import edu.kit.provideq.formats.gml.Node;

/**
 * Converts a {@link QaoaMaxCutReportFormat QAOA MaxCut report} into a
 * {@link GmlSolutionFormat GML solution} by attaching the solution bitstring as node
 * partition attributes.
 *
 * <p>The conversion requires the problem instance as context: the bitstring assigns
 * partitions to nodes by their order in the input graph, so the parsed input graph is
 * passed to {@link #convert(String, Gml)}.
 *
 * <p>The QAOA bits (0/1) are mapped to the partition convention (1/2) shared by the other
 * MaxCut solvers. Example conversion for a 3-node input graph:
 * <pre>
 * input:
 * energy:-1.8571428571428572
 * time:0.4310429096221924
 * max-cut objective:-1.8571428571428572
 * solution:[1. 0. 1.]
 * solution objective:8.0
 *
 * output:
 * graph [
 *   node [ id 1  partition 2 ]
 *   node [ id 2  partition 1 ]
 *   node [ id 3  partition 2 ]
 *   edge [ ... ]
 * ]
 * </pre>
 */
public final class QaoaReportToGmlSolutionConverter
    implements ContextConverter<String, String, Gml> {
  /**
   * Singleton instance of the converter.
   */
  public static final QaoaReportToGmlSolutionConverter INSTANCE =
      new QaoaReportToGmlSolutionConverter();

  private QaoaReportToGmlSolutionConverter() {
  }

  @Override
  public Format<String> getSource() {
    return QaoaMaxCutReportFormat.INSTANCE;
  }

  @Override
  public Format<String> getTarget() {
    return GmlSolutionFormat.INSTANCE;
  }

  @Override
  public Class<Gml> getContextClass() {
    return Gml.class;
  }

  @Override
  public String convert(String report, Gml inputGraph) throws FormatException {
    var solutionLine = report.lines()
        .filter(line -> line.startsWith(QaoaMaxCutReportFormat.SOLUTION_LINE_PREFIX))
        .findFirst()
        .orElseThrow(() -> new FormatException(
            "QAOA MaxCut report contains no '%s' line"
                .formatted(QaoaMaxCutReportFormat.SOLUTION_LINE_PREFIX)));

    // the bitstring is written as "[1. 1. 0.]" (numpy float array) or "[1 1 0]"
    // (numpy int array) depending on the qiskit version; strip the brackets and split
    // on any separator
    var bits = solutionLine
        .substring(QaoaMaxCutReportFormat.SOLUTION_LINE_PREFIX.length())
        .replace("[", "")
        .replace("]", "")
        .trim()
        .split("[.\\s]+");

    // attach partitions to a deep copy of the graph, so the input graph is not mutated
    var solutionNodes = inputGraph.nodes().stream().map(Node::new).toList();
    var solutionGraph = new Gml(solutionNodes, inputGraph.edges());
    for (int i = 0; i < bits.length && i < solutionGraph.nodes().size(); i++) {
      var bit = bits[i].trim();
      var partition = bit.equals("0") ? "1" : "2";
      solutionGraph.nodes().get(i).attributes().put("partition", partition);
    }

    return solutionGraph.toString();
  }
}
