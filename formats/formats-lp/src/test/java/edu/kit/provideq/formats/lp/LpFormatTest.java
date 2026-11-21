package edu.kit.provideq.formats.lp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Round-trip and assignment-parsing tests for the LP formats.
 */
class LpFormatTest {
  private static final String LP_SAMPLE = """
      Minimize
       obj: 0 + [ x1 * x2 ] / 2
      Subject To
      Binary
       x1 x2
      End
      """;

  static Stream<Arguments> formats() {
    return Stream.of(
        Arguments.of(LPFormat.INSTANCE, LP_SAMPLE),
        Arguments.of(LPSolutionFormat.INSTANCE, "x1 0\nx2 1\nx3 1\nx4 0\n"));
  }

  @ParameterizedTest
  @MethodSource("formats")
  void roundTripsSample(edu.kit.provideq.formats.core.Format<String> format, String sample)
      throws Exception {
    var parsed = FormattedValue.parse(sample, format);

    assertEquals(sample, parsed.toString());
  }

  /**
   * The LP solution format declares {@code <variable> <value>} assignments; valid notations
   * must parse and invalid lines must be rejected.
   */
  @ParameterizedTest
  @ValueSource(strings = {"x1 1\nx2 0\n", "x1=1\nx2=0\n", "x1 1.0\nx2 0.0\n"})
  void parsesValidLpSolutionAssignments(String text) throws ParseException {
    var assignments = LPSolutionFormat.parseAssignments(text);

    assertEquals(2, assignments.size());
    assertEquals(1.0, assignments.get("x1"));
    assertEquals(0.0, assignments.get("x2"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"x1: 1\n", "x1\n", "1 x1\n", "x1 one\n", "x1 1 extra\n"})
  void rejectsInvalidLpSolutionAssignments(String text) {
    assertThrows(ParseException.class, () -> LPSolutionFormat.parseAssignments(text));
  }

  @Test
  void parsesLpProblemAttributes() throws Exception {
    var parsed = FormattedValue.parse(LP_SAMPLE, LPFormat.INSTANCE);

    assertEquals(LP_SAMPLE, parsed.getValue());
  }
}
