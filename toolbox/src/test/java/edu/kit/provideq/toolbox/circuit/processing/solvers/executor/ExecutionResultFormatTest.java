package edu.kit.provideq.toolbox.circuit.processing.solvers.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.FormattedValue;
import java.util.Optional;

import edu.kit.provideq.toolbox.solvers.processing.solvers.executor.ExecutionResult;
import edu.kit.provideq.toolbox.solvers.processing.solvers.executor.ExecutionResultFormat;
import org.junit.jupiter.api.Test;

class ExecutionResultFormatTest {
  private static final String LF = "\n";

  @Test
  void roundTripsResultAndCircuit() throws Exception {
    var value = new ExecutionResult(Optional.of("{'00': 512}"), Optional.of("OPENQASM 2.0;"));
    var text = "Result:" + LF
        + "{'00': 512}"
        + LF
        + LF
        + "Circuit:" + LF
        + "OPENQASM 2.0;";
    var formatted = FormattedValue.of(value, ExecutionResultFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(value, FormattedValue.parse(text, ExecutionResultFormat.INSTANCE).getValue());
  }

  @Test
  void roundTripsResultOnly() throws Exception {
    var value = new ExecutionResult(Optional.of("{'00': 512}"), Optional.empty());
    var text = "Result:" + LF + "{'00': 512}";
    var formatted = FormattedValue.of(value, ExecutionResultFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(value, FormattedValue.parse(text, ExecutionResultFormat.INSTANCE).getValue());
  }

  @Test
  void roundTripsCircuitOnly() throws Exception {
    var value = new ExecutionResult(Optional.empty(), Optional.of("OPENQASM 2.0;"));
    var text = "Circuit:" + LF + "OPENQASM 2.0;";
    var formatted = FormattedValue.of(value, ExecutionResultFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(value, FormattedValue.parse(text, ExecutionResultFormat.INSTANCE).getValue());
  }
}
