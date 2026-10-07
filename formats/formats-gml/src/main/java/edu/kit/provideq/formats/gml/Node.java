package edu.kit.provideq.formats.gml;

import java.util.Map;

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
    this(other.id, other.label, new java.util.HashMap<>(other.attributes));
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
              .append(quote(label))
              .append(Gml.LINE_SEPARATOR);
    }

    // optional attributes
    for (var entry : attributes.entrySet()) {
      builder.append(Gml.INDENTATION_STEP)
              .append(entry.getKey())
              .append(Gml.SEPARATOR)
              .append(quote(entry.getValue()))
              .append(Gml.LINE_SEPARATOR);
    }

    // Add node end
    // ]
    builder.append(Gml.CLOSE)
            .append(Gml.LINE_SEPARATOR);

    return builder.toString();
  }

  static String quote(String value) {
    if (value.chars().allMatch(Character::isDigit) && !value.isEmpty()) {
      return value;
    }
    return '"' + value + '"';
  }

  static Node parseNode(GmlTokenizer tokenizer) {
    var id = -1;
    var label = "";
    var attributes = new java.util.HashMap<String, String>();

    while (!tokenizer.hasNext(Gml.CLOSE)) {
      String token = tokenizer.next();
      switch (token) {
        case Gml.ID_IDENTIFIER -> id = Integer.parseInt(tokenizer.next());
        case Gml.LABEL_IDENTIFIER -> label = stripQuotes(tokenizer.next());
        default -> attributes.put(token, stripQuotes(tokenizer.next()));
      }
    }

    return new Node(id, label, attributes);
  }

  static String stripQuotes(String value) {
    if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }
}
