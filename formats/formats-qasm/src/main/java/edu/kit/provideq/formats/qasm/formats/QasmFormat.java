package edu.kit.provideq.formats.qasm.formats;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

/**
 * The OpenQASM quantum circuit format.
 *
 * <p>The representation is the OpenQASM (2.0) source text; the format declares the
 * circuit structure so it can be addressed deterministically. Attribute readers can be
 * added as needed.
 *
 * <p>Example instance:
 * <pre>
 * OPENQASM 2.0;
 * include "qelib1.inc";
 * qreg q[2];
 * creg c[2];
 * h q[0];
 * cx q[0], q[1];
 * measure q -> c;
 * </pre>
 */
public record QasmFormat() implements Format<String> {
  /**
   * Singleton instance of the OpenQASM format.
   */
  public static final QasmFormat INSTANCE = new QasmFormat();

  @Override
  public String getName() {
    return "qasm";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("openqasm");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("qasm");
  }

  @Override
  public FormatType getType() {
    return FormatType.PROBLEM;
  }

  @Override
  public Class<String> getRepresentationClass() {
    return String.class;
  }

  @Override
  public String parse(Reader reader) throws ParseException {
    try {
      return FormatReader.readAll(reader);
    } catch (IOException e) {
      throw new ParseException("Could not read OpenQASM input", e);
    }
  }

  @Override
  public void serialize(String value, Writer writer) throws SerializationException {
    try {
      writer.write(value);
    } catch (IOException e) {
      throw new SerializationException("Could not serialize OpenQASM circuit", e);
    }
  }
}