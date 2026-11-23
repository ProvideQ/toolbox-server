package edu.kit.provideq.formats.tsplib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormattedValue;
import java.util.stream.Stream;

import edu.kit.provideq.formats.tsplib.formats.TSPLIBFormat;
import edu.kit.provideq.formats.tsplib.formats.TSPLIBSolutionFormat;
import edu.kit.provideq.formats.tsplib.formats.VRPFormat;
import edu.kit.provideq.formats.tsplib.formats.VRPSolutionFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Round-trip tests for the TSPLIB formats: parsing a sample and serializing it again must
 * reproduce the original text.
 */
class TsplibFormatTest {
  private static final String VRP_SAMPLE = """
      NAME : test
      TYPE : CVRP
      DIMENSION : 3
      EDGE_WEIGHT_TYPE : EUC_2D
      CAPACITY : 2
      NODE_COORD_SECTION
      1 0.00000 0.00000
      2 1.00000 0.50000
      3 0.50000 -1.00000
      DEMAND_SECTION
      1 0
      2 1
      3 2
      DEPOT_SECTION
      1
      -1
      """;

  private static final String TSPLIB_SAMPLE = """
      NAME : tiny
      TYPE : TSP
      DIMENSION : 3
      EDGE_WEIGHT_TYPE : EUC_2D
      NODE_COORD_SECTION
      1 0.0 0.0
      2 1.0 0.0
      3 0.0 1.0
      EOF
      """;

  static Stream<Arguments> formats() {
    return Stream.of(
        Arguments.of(VRPFormat.INSTANCE, VRP_SAMPLE),
        Arguments.of(VRPSolutionFormat.INSTANCE, "1\n2\n3\n1\n"),
        Arguments.of(TSPLIBFormat.INSTANCE, TSPLIB_SAMPLE),
        Arguments.of(TSPLIBSolutionFormat.INSTANCE, "TOUR_SECTION\n1\n2\n3\n-1\nEOF\n"));
  }

  @ParameterizedTest
  @MethodSource("formats")
  void roundTripsSample(Format<String> format, String sample) throws Exception {
    var parsed = FormattedValue.parse(sample, format);

    assertEquals(sample, parsed.toString());
  }
}
