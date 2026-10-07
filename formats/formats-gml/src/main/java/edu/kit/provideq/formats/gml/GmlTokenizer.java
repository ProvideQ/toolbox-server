package edu.kit.provideq.formats.gml;

import edu.kit.provideq.formats.core.ParseException;
import java.util.NoSuchElementException;

/**
 * Tokenizer for GML input.
 *
 * <p>Separates input into tokens by whitespace, while ignoring whitespace inside
 * double quotes in order to preserve labels. Performs a single pass over the input
 * without regex or backtracking.
 */
final class GmlTokenizer {
  private final String input;
  private int position;

  GmlTokenizer(String input) {
    this.input = input;
  }

  /**
   * Returns the next token without consuming it.
   */
  String peek() {
    var saved = position;
    try {
      skipWhitespace();
      if (position >= input.length()) {
        throw new NoSuchElementException("unexpected end of input");
      }

      return readToken();
    } finally {
      position = saved;
    }
  }

  /**
   * Returns true if the next token equals the given token.
   */
  boolean hasNext(String token) {
    try {
      return peek().equals(token);
    } catch (NoSuchElementException _) {
      return false;
    }
  }

  /**
   * Consumes and returns the next token.
   */
  String next() {
    skipWhitespace();
    if (position >= input.length()) {
      throw new NoSuchElementException("unexpected end of input");
    }

    return readToken();
  }

  /**
   * Consumes the next token and verifies that it equals the given token.
   */
  void expect(String token) throws ParseException {
    skipWhitespace();
    if (position >= input.length()) {
      throw new ParseException("Unexpected end of input, expected %s".formatted(token));
    }

    var start = position;
    var next = readToken();

    if (!next.equals(token)) {
      throw new ParseException(
          "Expected %s but found %s at position %d".formatted(token, next, start));
    }
  }

  private String readToken() {
    var start = position;
    var inQuotes = false;

    while (position < input.length()) {
      var c = input.charAt(position);
      if (c == '"') {
        inQuotes = !inQuotes;
      } else if (!inQuotes && Character.isWhitespace(c)) {
        break;
      }
      position++;
    }

    return input.substring(start, position);
  }

  private void skipWhitespace() {
    while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
      position++;
    }
  }
}
