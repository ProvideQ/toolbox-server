package edu.kit.provideq.toolbox.knapsack;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The knapsack problem format.
 *
 * <p>The first line contains the number of items, followed by one line per item in
 * {@code <id> <value> <weight>} notation. The last line contains the maximum capacity.
 *
 * <p>Example instance:
 * <pre>
 * 3
 * 1 10 5
 * 2 6 3
 * 3 5 2
 * 5
 * </pre>
 */
public final class KnapsackFormat implements Format<Document<KnapsackProblem>> {
  public static final KnapsackFormat INSTANCE = new KnapsackFormat();

  private KnapsackFormat() {
  }

  @Override
  public Map<String, AttributeReader<Document<KnapsackProblem>, ?>> getAttributeReaders() {
    return Map.of(
        "item_count", AttributeReader.of(Integer.class, problem -> problem.parse().items().size()),
        "capacity", AttributeReader.of(Integer.class, problem -> problem.parse().capacity()));
  }

  @Override
  public String getName() {
    return "knapsack";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("knapsack");
  }

  @Override
  public FormatType getType() {
    return FormatType.PROBLEM;
  }

  @Override
  public Class<KnapsackProblem> getRepresentationClass() {
    return KnapsackProblem.class;
  }

  @Override
  public Document<KnapsackProblem> parse(Reader reader) throws ParseException {
    try {
      // The returned document retains the raw input; it is parsed into items and capacity
      // lazily on Document.parse().
      return Document.of(FormatReader.readAll(reader), KnapsackFormat::parseProblem);
    } catch (IOException e) {
      throw new ParseException("Could not read knapsack input", e);
    }
  }

  @Override
  public void serialize(Document<KnapsackProblem> value, Writer writer)
      throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize knapsack problem", e);
    }
  }

  static KnapsackProblem parseProblem(String source) throws ParseException {
    List<KnapsackItem> items = new ArrayList<>();

    var lines = source.lines()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .toList();
    if (lines.size() < 2) {
      throw new ParseException(
          "Knapsack input must contain at least one item and the capacity");
    }

    var itemCount = parseNonNegativeInt(lines.getFirst(), "item count");
    if (lines.size() != itemCount + 2) {
      throw new ParseException("Expected %d item lines, but found %d"
          .formatted(itemCount, lines.size() - 2));
    }

    for (var line : lines.subList(1, itemCount + 1)) {
      var parts = line.split("\\s+");
      if (parts.length != 3) {
        throw new ParseException(
            "Item lines must be in '<id> <value> <weight>' notation, but was: " + line);
      }
      items.add(new KnapsackItem(
          parseNonNegativeInt(parts[0], "item id"),
          parseNonNegativeInt(parts[1], "item value"),
          parseNonNegativeInt(parts[2], "item weight")
      ));
    }

    var capacity = parseNonNegativeInt(lines.get(itemCount + 1), "capacity");
    return new KnapsackProblem(items, capacity);
  }

  private static int parseNonNegativeInt(String value, String name) throws ParseException {
    try {
      var parsed = Integer.parseInt(value);
      if (parsed < 0) {
        throw new ParseException(name + " must be non-negative, but was: " + value);
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw new ParseException("Invalid " + name + ": " + value, e);
    }
  }
}