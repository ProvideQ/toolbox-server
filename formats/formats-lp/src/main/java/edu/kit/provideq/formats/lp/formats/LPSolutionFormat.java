package edu.kit.provideq.formats.lp.formats;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Linear-programming assignment solution format emitted by QUBO solvers.
 *
 * <p>The representation is text: one assignment per line in {@code <variable> <value>} or
 * {@code <variable>=<value>} notation, as produced by the respective solver backends.
 * Example instance:
 * <pre>
 * x1 0
 * x2 1
 * x3 1
 * x4 0
 * </pre>
 *
 * <p>The format declares this structure so the data can be addressed deterministically;
 * attribute readers can be added as needed.
 */
@SuppressWarnings("AbbreviationAsWordInName")
public record LPSolutionFormat() implements Format<String> {
  /**
   * Pattern of a single assignment line: {@code <variable> <value>} or {@code <variable>=<value>}.
   */
  private static final Pattern ASSIGNMENT_LINE =
      Pattern.compile("\\s*([A-Za-z_]\\w*)\\s*[= ]\\s*([-+]?(?:\\d+\\.?\\d*|\\.\\d+)"
          + "(?:[eE][-+]?\\d+)?)\\s*");

  /**
   * Singleton instance of the LP solution format.
   */
  public static final LPSolutionFormat INSTANCE = new LPSolutionFormat();

  @Override
  public String getName() {
    return "lp-solution";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("lp-sol");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<String> getRepresentationClass() {
    return String.class;
  }

  @Override
  public String parse(Reader reader) throws ParseException {
    String text;
    try {
      text = FormatReader.readAll(reader);
    } catch (IOException e) {
      throw new ParseException("Could not read LP solution input", e);
    }
    parseAssignments(text);
    return text;
  }

  /**
   * Parses the assignments of an LP solution text: one {@code <variable> <value>} or
   * {@code <variable>=<value>} pair per line.
   *
   * @param text LP solution text
   * @return assignments in order of appearance
   * @throws ParseException if a line does not declare a variable assignment
   */
  public static Map<String, Double> parseAssignments(String text) throws ParseException {
    var assignments = new LinkedHashMap<String, Double>();
    var matcher = ASSIGNMENT_LINE.matcher(text);
    int line = 0;
    for (var rawLine : text.split("\r?\n", -1)) {
      line++;
      var trimmed = rawLine.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (!matcher.reset(trimmed).matches()) {
        throw new ParseException(("Invalid LP solution assignment in line %d: \"%s\" "
            + "(expected \"<variable> <value>\" or \"<variable>=<value>\")")
            .formatted(line, trimmed));
      }
      assignments.put(matcher.group(1), Double.parseDouble(matcher.group(2)));
    }
    return assignments;
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize LP solution", e);
    }
  }
}