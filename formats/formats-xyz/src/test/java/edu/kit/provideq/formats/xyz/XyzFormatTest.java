package edu.kit.provideq.formats.xyz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.xyz.formats.XYZMoleculeFormat;
import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for the XYZ molecule format.
 */
class XyzFormatTest {
  @Test
  void roundTripsSample() throws Exception {
    var sample = "H 0 0 0; H 0 0 0.735";

    var parsed = FormattedValue.parse(sample, XYZMoleculeFormat.INSTANCE);

    assertEquals(sample, parsed.toString());
  }
}
