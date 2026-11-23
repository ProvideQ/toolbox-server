package edu.kit.provideq.formats.knapsack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.knapsack.formats.KnapsackFormat;
import edu.kit.provideq.formats.knapsack.formats.KnapsackSolutionFormat;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Round-trip tests for the knapsack formats: parsing a sample and serializing it again must
 * reproduce the original text.
 */
class KnapsackFormatTextTest {
  static Stream<Arguments> formats() {
    return Stream.of(
        Arguments.of(KnapsackFormat.INSTANCE, "2\n1 5 5\n2 3 4\n9\n"),
        Arguments.of(KnapsackSolutionFormat.INSTANCE, "42\n1\n"));
  }

  @ParameterizedTest
  @MethodSource("formats")
  void roundTripsSample(Format<String> format, String sample) throws Exception {
    var parsed = FormattedValue.parse(sample, format);

    assertEquals(sample, parsed.toString());
  }
}
