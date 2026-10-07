package edu.kit.provideq.formats.gml;

import java.util.Map;

/**
 * Object representation of a GML edge.
 *
 * @param source     source node of the edge
 * @param target     target node of the edge
 * @param attributes optional custom attributes of the edge
 */
public record Edge(int source, int target, Map<String, String> attributes) {
  /**
   * Copy constructor.
   *
   * @param other edge to copy
   */
  public Edge(Edge other) {
    this(other.source, other.target, new java.util.HashMap<>(other.attributes));
  }

  static Edge parseEdge(GmlTokenizer tokenizer) {
    var source = -1;
    var target = -1;
    var attributes = new java.util.HashMap<String, String>();

    while (!tokenizer.hasNext(Gml.CLOSE)) {
      String token = tokenizer.next();
      switch (token) {
        case Gml.SOURCE_IDENTIFIER -> source = Integer.parseInt(tokenizer.next());
        case Gml.TARGET_IDENTIFIER -> target = Integer.parseInt(tokenizer.next());
        default -> attributes.put(token, Node.stripQuotes(tokenizer.next()));
      }
    }

    return new Edge(source, target, attributes);
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    // Add edge start
    // edge [
    builder.append(Gml.EDGE_IDENTIFIER)
            .append(Gml.SEPARATOR)
            .append(Gml.OPEN)
            .append(Gml.LINE_SEPARATOR);

    // source
    builder.append(Gml.INDENTATION_STEP)
            .append(Gml.SOURCE_IDENTIFIER)
            .append(Gml.SEPARATOR)
            .append(source)
            .append(Gml.LINE_SEPARATOR);

    // target
    builder.append(Gml.INDENTATION_STEP)
            .append(Gml.TARGET_IDENTIFIER)
            .append(Gml.SEPARATOR)
            .append(target)
            .append(Gml.LINE_SEPARATOR);

    // optional attributes
    for (var entry : attributes.entrySet()) {
      builder.append(Gml.INDENTATION_STEP)
              .append(entry.getKey())
              .append(Gml.SEPARATOR)
              .append(Node.quote(entry.getValue()))
              .append(Gml.LINE_SEPARATOR);
    }

    // Add edge end
    // ]
    builder.append(Gml.CLOSE)
            .append(Gml.LINE_SEPARATOR);

    return builder.toString();
  }
}
