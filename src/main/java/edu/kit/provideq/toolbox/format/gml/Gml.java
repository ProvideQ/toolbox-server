package edu.kit.provideq.toolbox.format.gml;

import edu.kit.provideq.toolbox.exception.ConversionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Object representation of a GML file.
 */
public class Gml {
  static final String LINE_SEPARATOR = System.lineSeparator();
  static final String SEPARATOR = " ";
  static final String GRAPH_IDENTIFIER = "graph";
  static final String NODE_IDENTIFIER = "node";
  static final String ID_IDENTIFIER = "id";
  static final String LABEL_IDENTIFIER = "label";
  static final String EDGE_IDENTIFIER = "edge";
  static final String SOURCE_IDENTIFIER = "source";
  static final String TARGET_IDENTIFIER = "target";
  static final String OPEN = "\\[";
  static final String CLOSE = "\\]";

  /**
   * Separates a string into tokens by splitting by whitespaces,
   * while ignoring whitespace inside quotes in order to preserve labels.
   */
  static final Pattern TokenPattern = Pattern.compile("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

  private final List<Node> nodes;
  private final List<Edge> edges;

  /**
   * Creates a GML object from a list of nodes and edges.
   *
   * @param nodes nodes of the graph
   * @param edges edges that connect the nodes
   */
  public Gml(List<Node> nodes, List<Edge> edges) {
    this.nodes = nodes
            .stream()
            .map(Node::new)
            .toList();

    this.edges = edges
            .stream()
            .map(Edge::new)
            .toList();
  }

  /**
   * Copy constructor.
   *
   * @param other GML object to copy
   */
  public Gml(Gml other) {
    this(other.nodes, other.edges);
  }

  /**
   * Creates a GML object from a string.
   *
   * @param gmlString GML string
   * @return GML object
   * @throws ConversionException if the GML string could not be parsed
   */
  public static Gml fromString(String gmlString) throws ConversionException {
    var scanner = new Scanner(gmlString);
    scanner.useDelimiter(TokenPattern);

    try (scanner) {
      return parseGraph(scanner);
    } catch (Exception e) {
      throw new ConversionException("Could not parse GML string:\n", e);
    }
  }

  private static Gml parseGraph(Scanner scanner) throws ConversionException {
    // graph
    scanner.next(Gml.GRAPH_IDENTIFIER);

    // [
    scanner.next(Gml.OPEN);

    // nodes and edges
    var nodes = new ArrayList<Node>();
    var edges = new ArrayList<Edge>();

    while (!scanner.hasNext(Gml.CLOSE)) {
      String token = scanner.next();

      switch (token) {
        case Gml.NODE_IDENTIFIER -> {
          scanner.next(Gml.OPEN);
          nodes.add(Node.parseNode(scanner));
          scanner.next(Gml.CLOSE);
        }
        case Gml.EDGE_IDENTIFIER -> {
          scanner.next(Gml.OPEN);
          edges.add(Edge.parseEdge(scanner));
          scanner.next(Gml.CLOSE);
        }
        case Gml.OPEN -> {
          throw new ConversionException("Unexpected opening bracket in node/edge list");
        }
        case Gml.CLOSE -> {
          throw new ConversionException("Unexpected closing bracket in node/edge list");
        }
        case Gml.GRAPH_IDENTIFIER -> {
          throw new ConversionException("Unexpected graph identifier in node/edge list");
        }
        default -> {
          // ignore custom attributes for the graph directly
        }
      }
    }

    // ]
    scanner.next(Gml.CLOSE);

    return new Gml(nodes, edges);
  }

  /**
   * Gets the nodes of the GML object.
   *
   * @return nodes of the GML object
   */
  public List<Node> getNodes() {
    return nodes;
  }

  /**
   * Gets the edges of the GML object.
   *
   * @return edges of the GML object
   */
  public List<Edge> getEdges() {
    return edges;
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    // Add graph start
    // graph [
    builder.append(GRAPH_IDENTIFIER)
            .append(SEPARATOR)
            .append(OPEN)
            .append(LINE_SEPARATOR);

    for (var node : nodes) {
      builder.append(node.toString());
    }

    for (var edge : edges) {
      builder.append(edge.toString());
    }

    // Add graph end
    // ]
    builder.append(CLOSE);

    return builder.toString();
  }
}
