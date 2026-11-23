package edu.kit.provideq.toolbox.solvers.circuit_processing.solvers.executor;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Optional;
import java.util.Set;

/**
 * The quantum circuit execution result format.
 *
 * <p>The representation contains an optional {@code Result:} section with the measurement
 * outcomes and an optional {@code Circuit:} section with the executed circuit, separated
 * by a blank line.
 *
 * <p>Example instance:
 * <pre>
 * Result:
 * {'00': 512, '11': 512}
 *
 * Circuit:
 * OPENQASM 2.0;
 * include "qelib1.inc";
 * qreg q[2];
 * creg c[2];
 * h q[0];
 * cx q[0], q[1];
 * </pre>
 */
public final class ExecutionResultFormat implements Format<ExecutionResult> {
  public static final ExecutionResultFormat INSTANCE = new ExecutionResultFormat();

  private static final String RESULT_HEADER = "Result:";
  private static final String CIRCUIT_HEADER = "Circuit:";
  private static final String SECTION_SEPARATOR = "\n\n";

  private ExecutionResultFormat() {
  }

  @Override
  public String getName() {
    return "execution-result";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of();
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("exec-result");
  }

  @Override
  public FormatType getType() {
    return FormatType.SOLUTION;
  }

  @Override
  public Class<ExecutionResult> getRepresentationClass() {
    return ExecutionResult.class;
  }

  @Override
  public ExecutionResult parse(Reader reader) throws ParseException {
    Optional<String> result = Optional.empty();
    Optional<String> circuit = Optional.empty();
    try {
      var text = FormatReader.readAll(reader);
      if (text.isBlank()) {
        return new ExecutionResult(result, circuit);
      }

      var circuitIndex = text.lastIndexOf(SECTION_SEPARATOR + CIRCUIT_HEADER + "\n");
      if (circuitIndex >= 0) {
        circuit = Optional.of(text.substring(circuitIndex
            + SECTION_SEPARATOR.length()
            + CIRCUIT_HEADER.length()
            + 1));
        text = text.substring(0, circuitIndex);
      } else if (text.startsWith(CIRCUIT_HEADER + "\n")) {
        circuit = Optional.of(text.substring(CIRCUIT_HEADER.length() + 1));
        text = "";
      }

      if (text.startsWith(RESULT_HEADER + "\n")) {
        result = Optional.of(text.substring(RESULT_HEADER.length() + 1));
      } else if (!text.isBlank()) {
        throw new ParseException(
            "Execution result must start with '" + RESULT_HEADER + "', but was: " + text);
      }

      return new ExecutionResult(result, circuit);
    } catch (IOException e) {
      throw new ParseException("Could not read execution result", e);
    }
  }

  @Override
  public void serialize(ExecutionResult value, Writer writer) throws SerializationException {
    var builder = new StringBuilder();
    value.resultString().ifPresent(result -> builder
        .append(RESULT_HEADER)
        .append('\n')
        .append(result));
    value.circuit().ifPresent(circuit -> {
      if (!builder.isEmpty()) {
        builder.append(SECTION_SEPARATOR);
      }
      builder.append(CIRCUIT_HEADER).append('\n').append(circuit);
    });

    try {
      writer.write(builder.toString());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize execution result", e);
    }
  }
}