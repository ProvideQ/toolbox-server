package edu.kit.provideq.toolbox.format.gml;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Object representation of a GML node.
 *
 * @param id         id of the node
 * @param label      optional label of the node
 * @param attributes optional custom attributes of the node
 */
public record Node(int id, String label, Map<String, String> attributes) {
  /**
   * Copy constructor.
   *
   * @param other node to copy
   */
  public Node(Node other) {
    this(other.id, other.label, new HashMap<>(other.attributes));
  }

  static Node parseNode(Scanner scanner) {
    var id = -1;
    var label = "";
    var attributes = new HashMap<String, String>();

    while (!scanner.hasNext(Gml.CLOSE)) {
      String token = scanner.next();
      switch (token) {
        case Gml.ID_IDENTIFIER -> id = scanner.nextInt();
        case Gml.LABEL_IDENTIFIER -> label = scanner.next();
        default -> attributes.put(token, scanner.next());
      }
    }

    return new Node(id, label, attributes);
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    // Add node start
    // node [
    builder.append(Gml.NODE_IDENTIFIER)
            .append(Gml.SEPARATOR)
            .append(Gml.OPEN)
            .append(Gml.LINE_SEPARATOR);

    // id
    builder.append(Gml.INDENTATION_STEP)
            .append(Gml.ID_IDENTIFIER)
            .append(Gml.SEPARATOR)
            .append(id)
            .append(Gml.LINE_SEPARATOR);

    // optional label
    if (label != null && !label.isEmpty()) {
      builder.append(Gml.INDENTATION_STEP)
              .append(Gml.LABEL_IDENTIFIER)
              .append(Gml.SEPARATOR)
              .append(label)
              .append(Gml.LINE_SEPARATOR);
    }

    // optional attributes
    for (var entry : attributes.entrySet()) {
      builder.append(Gml.INDENTATION_STEP)
              .append(entry.getKey())
              .append(Gml.SEPARATOR)
              .append(entry.getValue())
              .append(Gml.LINE_SEPARATOR);
    }

    // Add node end
    // ]
    builder.append(Gml.CLOSE)
            .append(Gml.LINE_SEPARATOR);

    return builder.toString();
  }
}
