package edu.kit.provideq.formats.core;

import edu.kit.provideq.formats.core.exceptions.ParseException;
import java.util.Objects;

/**
 * String representation of a structured object that lazily provides the structured object.
 *
 * <p>The document retains the raw text of a format; {@link #parse()} is the single operation
 * that parses it into the structured representation using the parser function supplied at
 * creation. Structured information can then be accessed without handling a parse exception
 * on every access.
 *
 * <p>As long as the document has not been parsed, {@link #toString()} returns the original
 * source text unchanged. Once it has been parsed, {@link #toString()} serializes the
 * structured representation instead.
 *
 * @param <T> structured representation produced by {@link #parse()}
 */
public final class Document<T> {
  /**
   * Parses a source string into the structured representation.
   *
   * @param <T> structured representation
   */
  @FunctionalInterface
  public interface Parser<T> {
    T parse(String source) throws ParseException;
  }

  private final String source;
  private final Parser<T> parser;
  private T parsed;

  private Document(String source, Parser<T> parser) {
    this.source = Objects.requireNonNull(source, "source");
    this.parser = Objects.requireNonNull(parser, "parser");
  }

  private Document(T parsed) {
    this.source = null;
    this.parser = null;
    this.parsed = Objects.requireNonNull(parsed, "parsed");
  }

  /**
   * Creates a document from a source string without parsing it.
   *
   * @param source raw text of the format
   * @param parser function that parses the source into the structured representation
   * @param <T> structured representation
   * @return document
   */
  public static <T> Document<T> of(String source, Parser<T> parser) {
    return new Document<>(source, parser);
  }

  /**
   * Wraps an already structured object in a document.
   *
   * @param parsed structured representation
   * @param <T> structured representation
   * @return document
   */
  public static <T> Document<T> of(T parsed) {
    return new Document<>(parsed);
  }

  /**
   * Returns the structured representation, parsing the source string on first call.
   *
   * @return structured representation
   * @throws ParseException if the source string could not be parsed
   */
  public T parse() throws ParseException {
    if (parsed == null) {
      if (parser != null) {
        parsed = parser.parse(source);
      } else {
        throw new ParseException("no parser available");
      }
    }

    return parsed;
  }

  /**
   * Returns the text of the document.
   *
   * <p>If the document has not been parsed yet, the original source is returned unchanged.
   * Otherwise, the structured representation is serialized.
   */
  @Override
  public String toString() {
    return parsed != null ? parsed.toString() : source;
  }
}
