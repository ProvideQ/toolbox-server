package edu.kit.provideq.formats.gml;

import edu.kit.provideq.formats.core.ParseException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Object representation of a GML file.
 */
public record Gml(List<Node> nodes, List<Edge> edges) {
  static final String LINE_SEPARATOR = System.lineSeparator();
  static final String SEPARATOR = " ";
  static final String GRAPH_IDENTIFIER = "graph";
  static final String NODE_IDENTIFIER = "node";
  static final String ID_IDENTIFIER = "id";
  static final String LABEL_IDENTIFIER = "label";
  static final String EDGE_IDENTIFIER = "edge";
  static final String SOURCE_IDENTIFIER = "source";
  static final String TARGET_IDENTIFIER = "target";
  static final String OPEN = "[";
  static final String CLOSE = "]";
  static final String INDENTATION_STEP = "  ";

  /**
   * Creates a GML object from a list of nodes and edges.
   *
   * @param nodes nodes of the graph
   * @param edges edges that connect the nodes
   */
  public Gml(List<Node> nodes, List<Edge> edges) {
    this.nodes = List.copyOf(nodes);
    this.edges = List.copyOf(edges);
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
   * @throws ParseException if the GML string could not be parsed
   */
  public static Gml fromString(String gmlString) throws ParseException {
    var tokenizer = new GmlTokenizer(gmlString);

    try {
      return parseGraph(tokenizer);
    } catch (ParseException e) {
      throw e;
    } catch (Exception e) {
      throw new ParseException("Could not parse GML string", e);
    }
  }

  /**
   * Creates a GML object from a reader.
   *
   * @param reader GML input
   * @return GML object
   * @throws ParseException if the GML input could not be parsed
   */
  public static Gml fromReader(Reader reader) throws ParseException {
    var builder = new StringBuilder();
    try (reader) {
      char[] buffer = new char[8192];
      int read;
      while ((read = reader.read(buffer)) != -1) {
        builder.append(buffer, 0, read);
      }
    } catch (IOException e) {
      throw new ParseException("Could not read GML input", e);
    }

    return fromString(builder.toString());
  }

  private static Gml parseGraph(GmlTokenizer tokenizer) throws ParseException {
    // graph
    tokenizer.expect(Gml.GRAPH_IDENTIFIER);

    // [
    tokenizer.expect(Gml.OPEN);

    // nodes and edges
    List<Node> nodes = new ArrayList<>();
    List<Edge> edges = new ArrayList<>();

    while (!tokenizer.hasNext(Gml.CLOSE)) {
      String token = tokenizer.next();

      switch (token) {
        case Gml.NODE_IDENTIFIER -> {
          tokenizer.expect(Gml.OPEN);
          nodes.add(Node.parseNode(tokenizer));
          tokenizer.expect(Gml.CLOSE);
        }
        case Gml.EDGE_IDENTIFIER -> {
          tokenizer.expect(Gml.OPEN);
          edges.add(Edge.parseEdge(tokenizer));
          tokenizer.expect(Gml.CLOSE);
        }
        case Gml.OPEN -> throw new ParseException("Unexpected opening bracket in node/edge list");
        case Gml.CLOSE -> throw new ParseException("Unexpected closing bracket in node/edge list");
        case Gml.GRAPH_IDENTIFIER ->
            throw new ParseException("Unexpected graph identifier in node/edge list");
        default -> // ignore custom attributes for the graph directly
            tokenizer.next();
      }
    }

    // ]
    tokenizer.expect(Gml.CLOSE);

    return new Gml(nodes, edges);
  }

  /**
   * Gets the nodes of the GML object.
   *
   * @return nodes of the GML object
   */
  @Override
  public List<Node> nodes() {
    return nodes;
  }

  /**
   * Gets the edges of the GML object.
   *
   * @return edges of the GML object
   */
  @Override
  public List<Edge> edges() {
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
      appendIndented(builder, node.toString());
    }

    for (var edge : edges) {
      appendIndented(builder, edge.toString());
    }

    // Add graph end
    // ]
    builder.append(CLOSE);

    return builder.toString();
  }

  private static void appendIndented(StringBuilder builder, String block) {
    block.lines()
        .map(line -> INDENTATION_STEP + line + LINE_SEPARATOR)
        .forEach(builder::append);
  }
}
