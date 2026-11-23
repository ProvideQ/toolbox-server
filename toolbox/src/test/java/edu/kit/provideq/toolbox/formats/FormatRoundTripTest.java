package edu.kit.provideq.toolbox.formats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.gml.formats.GmlSolutionFormat;
import edu.kit.provideq.formats.lp.formats.LPSolutionFormat;
import edu.kit.provideq.toolbox.demonstrators.DemonstratorInputFormat;
import edu.kit.provideq.toolbox.demonstrators.DemonstratorReportFormat;
import edu.kit.provideq.toolbox.solvers.material_simulation.formats.MoleculeEnergySolutionFormat;
import edu.kit.provideq.toolbox.solvers.qubo.formats.JsonQuboSolutionFormat;
import edu.kit.provideq.toolbox.solvers.qubo.formats.converters.JsonQuboToLpSolutionConverter;
import edu.kit.provideq.toolbox.solvers.qubo.formats.converters.LpSolutionToJsonQuboConverter;
import edu.kit.provideq.toolbox.solvers.unsplittable_mcf.formats.UmcfSolutionFormat;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Round-trip tests for formats that had no format tests: parsing a sample and serializing it
 * again must reproduce the original text.
 */
class FormatRoundTripTest {
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
        Arguments.of(GmlSolutionFormat.INSTANCE, GML_SOLUTION_SAMPLE),
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
