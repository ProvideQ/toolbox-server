package edu.kit.provideq.formats.core;

import java.io.IOException;
import java.io.Reader;

/**
 * Shared utilities for format implementations.
 */
public final class FormatReader {
  private FormatReader() {
  }

  /**
   * Reads all remaining input from the given reader into a string,
   * using an 8 KiB buffer without intermediate per-chunk String allocation overhead
   * beyond the builder's capacity growth.
   *
   * @param reader input to read
   * @return the full input as a string
   * @throws IOException if reading failed
   */
  public static String readAll(Reader reader) throws IOException {
    var builder = new StringBuilder();
    char[] buffer = new char[8192];
    int read;
    while ((read = reader.read(buffer)) != -1) {
      builder.append(buffer, 0, read);
    }
    return builder.toString();
  }
}
