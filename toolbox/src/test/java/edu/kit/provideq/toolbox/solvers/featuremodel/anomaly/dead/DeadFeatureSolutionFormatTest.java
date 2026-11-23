package edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.FormattedValue;
import java.util.List;

import org.junit.jupiter.api.Test;

class DeadFeatureSolutionFormatTest {
  @Test
  void roundTripsDeadFeatureSolutionText() throws Exception {
    var value = new DeadFeatureSolution(List.of("Battery", "Screen"));
    var text = "Battery" + System.lineSeparator() + "Screen" + System.lineSeparator();
    var formatted = FormattedValue.of(value, DeadFeatureSolutionFormat.INSTANCE);

    assertEquals(text, formatted.toString());
    assertEquals(value, FormattedValue.parse(text, DeadFeatureSolutionFormat.INSTANCE).getValue());
  }
}
