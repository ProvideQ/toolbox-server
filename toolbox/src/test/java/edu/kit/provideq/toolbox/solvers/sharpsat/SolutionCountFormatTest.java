package edu.kit.provideq.toolbox.solvers.sharpsat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.toolbox.solvers.sharpsat.formats.SolutionCount;
import edu.kit.provideq.toolbox.solvers.sharpsat.formats.SolutionCountFormat;
import org.junit.jupiter.api.Test;

class SolutionCountFormatTest {
  @Test
  void roundTripsSolutionCount() throws Exception {
    var value = new SolutionCount(42);
    var text = "count: 42" + System.lineSeparator();
    var formatted = FormattedValue.of(value, SolutionCountFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(value, FormattedValue.parse(text, SolutionCountFormat.INSTANCE).getValue());
  }
}
