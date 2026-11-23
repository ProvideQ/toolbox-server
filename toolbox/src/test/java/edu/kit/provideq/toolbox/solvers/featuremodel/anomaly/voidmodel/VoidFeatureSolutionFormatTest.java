package edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.voidmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.voidmodel.formats.VoidFeatureSolution;
import edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.voidmodel.formats.VoidFeatureSolutionFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VoidFeatureSolutionFormatTest {
  @Test
  void roundTripsVoidSolution() throws Exception {
    var value = new VoidFeatureSolution(true, Map::of);
    var text = "void: true" + System.lineSeparator();
    var formatted = FormattedValue.of(value, VoidFeatureSolutionFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertRoundTrip(value, text);
  }

  @Test
  void roundTripsNonVoidSolutionWithExampleConfiguration() throws Exception {
    var value = new VoidFeatureSolution(false, () -> Map.of("Battery", true, "Screen", false));
    var text = "void: false" + System.lineSeparator()
        + "Battery: true" + System.lineSeparator()
        + "Screen: false" + System.lineSeparator();
    var formatted = FormattedValue.of(value, VoidFeatureSolutionFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertRoundTrip(value, text);
  }

  private static void assertRoundTrip(VoidFeatureSolution value, String text) throws Exception {
    var parsed = FormattedValue.parse(text, VoidFeatureSolutionFormat.INSTANCE).getValue();
    assertEquals(value.isVoid(), parsed.isVoid());
    assertEquals(value.exampleConfiguration().get(), parsed.exampleConfiguration().get());
  }
}
