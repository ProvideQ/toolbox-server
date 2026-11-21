package edu.kit.provideq.formats.qasm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.FormattedValue;
import org.junit.jupiter.api.Test;

class QasmFormatTest {
  @Test
  void roundTripsCircuit() throws Exception {
    var circuit = "OPENQASM 2.0;" + System.lineSeparator()
        + "include \"qelib1.inc\";" + System.lineSeparator()
        + "h q[0];" + System.lineSeparator();
    var formatted = FormattedValue.of(circuit, QasmFormat.INSTANCE);

    assertEquals(circuit, formatted.toString());
    assertEquals(circuit, FormattedValue.parse(circuit, QasmFormat.INSTANCE).getValue());
  }
}
