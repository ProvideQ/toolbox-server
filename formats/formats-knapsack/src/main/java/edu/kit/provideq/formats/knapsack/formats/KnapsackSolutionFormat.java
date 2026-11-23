package edu.kit.provideq.formats.knapsack.formats;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import edu.kit.provideq.formats.knapsack.formats.representation.KnapsackSolution;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The knapsack solution format.
 *
 * <p>The first line contains the total value of the selected items, followed by one line
 * with the identifier of each selected item.
 * <pre>
 * 42
 * 1
 * 3
 * 5
 * </pre>
 */
public final class KnapsackSolutionFormat implements Format<Document<KnapsackSolution>> {
  public static final KnapsackSolutionFormat INSTANCE = new KnapsackSolutionFormat();

  private KnapsackSolutionFormat() {
  }

  @Override
  public Map<String, AttributeReader<Document<KnapsackSolution>, ?>> getAttributeReaders() {
    return Map.of(
        "total_value",
        AttributeReader.of(Integer.class, solution -> solution.parse().totalValue()),
        "selected_item_count",
        AttributeReader.of(Integer.class, solution -> solution.parse().selectedItemIds().size()));
  }

  @Override
  public String getName() {
    return "knapsack-solution";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("knapsack-sol");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<KnapsackSolution> getRepresentationClass() {
    return KnapsackSolution.class;
  }

  @Override
  public Document<KnapsackSolution> parse(Reader reader) throws ParseException {
    try {
      // The returned document retains the raw input; it is parsed into total value and
      // selected items lazily on Document.parse().
      return Document.of(FormatReader.readAll(reader), KnapsackSolutionFormat::parseSolution);
    } catch (IOException e) {
      throw new ParseException("Could not read knapsack solution", e);
    }
  }

  @Override
  public void serialize(Document<KnapsackSolution> value, Writer writer)
      throws SerializationException {
    try {
      writer.write(value.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize knapsack solution", e);
    }
  }

  static KnapsackSolution parseSolution(String source) throws ParseException {
    List<Integer> selectedItemIds = new ArrayList<>();

    var lines = source.lines()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .toList();
    if (lines.isEmpty()) {
      throw new ParseException("Knapsack solution must contain the total value");
    }

    int totalValue;
    try {
      totalValue = Integer.parseInt(lines.getFirst());
    } catch (NumberFormatException e) {
      throw new ParseException("Invalid total value: " + lines.getFirst(), e);
    }

    for (var line : lines.subList(1, lines.size())) {
      try {
        selectedItemIds.add(Integer.parseInt(line));
      } catch (NumberFormatException e) {
        throw new ParseException("Invalid selected item id: " + line, e);
      }
    }

    return new KnapsackSolution(totalValue, selectedItemIds);
  }
}