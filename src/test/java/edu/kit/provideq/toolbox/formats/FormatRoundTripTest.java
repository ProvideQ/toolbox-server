package edu.kit.provideq.toolbox.formats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.toolbox.demonstrators.DemonstratorInputFormat;
import edu.kit.provideq.toolbox.demonstrators.DemonstratorReportFormat;
import edu.kit.provideq.toolbox.materialsimulation.MoleculeEnergySolutionFormat;
import edu.kit.provideq.toolbox.qubo.JsonQuboSolutionFormat;
import edu.kit.provideq.toolbox.qubo.JsonQuboToLpSolutionConverter;
import edu.kit.provideq.toolbox.qubo.LpSolutionToJsonQuboConverter;
import edu.kit.provideq.toolbox.unsplittablemcf.UmcfSolutionFormat;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Round-trip tests for formats that had no format tests: parsing a sample and serializing it
 * again must reproduce the original text.
 */
class FormatRoundTripTest {
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

  private static final String LP_SAMPLE = """
      Minimize
       obj: 0 + [ x1 * x2 ] / 2
      Subject To
      Binary
       x1 x2
      End
      """;

  private static final String GML_SOLUTION_SAMPLE = """
      graph [
        node [
          id 0
          label "0"
          partition 1
        ]
        node [
          id 1
          label "1"
          partition 2
        ]
        edge [
          source 0
          target 1
          weight 1
        ]
      ]""";

  static Stream<Arguments> formats() {
    return Stream.of(
        Arguments.of(VRPFormat.INSTANCE, VRP_SAMPLE),
        Arguments.of(VRPSolutionFormat.INSTANCE, "1\n2\n3\n1\n"),
        Arguments.of(TSPLIBFormat.INSTANCE, TSPLIB_SAMPLE),
        Arguments.of(TSPLIBSolutionFormat.INSTANCE, "TOUR_SECTION\n1\n2\n3\n-1\nEOF\n"),
        Arguments.of(LPFormat.INSTANCE, LP_SAMPLE),
        Arguments.of(edu.kit.provideq.formats.gml.GmlSolutionFormat.INSTANCE, GML_SOLUTION_SAMPLE),
        Arguments.of(DemonstratorInputFormat.INSTANCE, "demonstrator payload\n"),
        Arguments.of(DemonstratorReportFormat.INSTANCE, "demonstrator report\n"),
        Arguments.of(MoleculeEnergySolutionFormat.INSTANCE, "-1.0579\n"),
        Arguments.of(UmcfSolutionFormat.INSTANCE, "<html><body>report</body></html>\n"),
        Arguments.of(
            JsonQuboSolutionFormat.INSTANCE,
            "{\n    \"x1\": 1.0,\n    \"x4\": 1.0\n}"));
  }

  @ParameterizedTest
  @MethodSource("formats")
  void roundTripsSample(Format<String> format, String sample) throws Exception {
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

  /**
   * The lp-solution and sparse QUBO solution formats are mutually convertible: converting
   * forth and back must reproduce the active (1-valued) assignments.
   */
  @org.junit.jupiter.api.Test
  void roundTripsLpSolutionThroughSparseQubo() throws Exception {
    var lpSolution = "x1 0\nx2 1\nx3 0\nx4 1\n";
    var registry = new edu.kit.provideq.formats.core.converters.ConversionRegistry();
    registry.registerConverter(
        LpSolutionToJsonQuboConverter.INSTANCE);
    registry.registerConverter(
        JsonQuboToLpSolutionConverter.INSTANCE);
    var service = new edu.kit.provideq.formats.core.converters.ConversionService(registry);

    var sparse = LpSolutionToJsonQuboConverter.INSTANCE
        .convert(lpSolution);
    var parsedSparse = FormattedValue.parse(sparse,
        JsonQuboSolutionFormat.INSTANCE);
    var roundTrip = parsedSparse.as(LPSolutionFormat.INSTANCE, service);

    assertEquals("x2 1\nx4 1\n", roundTrip);
  }
}
