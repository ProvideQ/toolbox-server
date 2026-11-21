package edu.kit.provideq.formats.gml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.attributes.AttributeService;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the GML module: parsing, round-trip and properties.
 */
class GmlTest {
  @ParameterizedTest
  @MethodSource("testToStringParameters")
  void testToString(String gmlString) throws FormatException {
    gmlString = gmlString.replace("\n", System.lineSeparator());

    var gml = Gml.fromString(gmlString);
    assertEquals(gmlString, gml.toString());
  }

  static Stream<String> testToStringParameters() {
    return Stream.of(
            """
                    graph [
                      node [
                        id 1
                        label "Node 1"
                      ]
                      node [
                        id 2
                        label "Node 2"
                      ]
                      edge [
                        source 1
                        target 2
                        label "Edge from Node 1 to Node 2"
                      ]
                    ]""", """
                    graph [
                      node [
                        id 1
                        label "node 1"
                        testAttributeXY "This is a test"
                        thisIsASampleAttribute 42
                      ]
                      node [
                        id 2
                        label "node 2"
                      ]
                      node [
                        id 3
                        label "node 3"
                      ]
                      edge [
                        source 1
                        target 2
                        testAttributeXY "This is a test"
                        label "Edge from node 1 to node 2"
                      ]
                      edge [
                        source 2
                        target 3
                        label "Edge from node 2 to node 3"
                      ]
                      edge [
                        source 3
                        target 1
                        label "Edge from node 3 to node 1"
                      ]
                    ]""", """
                    graph [
                      node [
                        id 1
                      ]
                      node [
                        id 2
                      ]
                      node [
                        id 3
                      ]
                      node [
                        id 4
                      ]
                      edge [
                        source 1
                        target 2
                      ]
                      edge [
                        source 2
                        target 3
                      ]
                      edge [
                        source 3
                        target 4
                      ]
                    ]"""
    );
  }

  @Test
  void readsAttributes() throws FormatException {
    var registry = new ConversionRegistry();
    GmlFormats.register(registry);
    registry.seal();
    var attributeService = new AttributeService(registry, new ConversionService(registry));

    var gml = Document.of(Gml.fromString("""
        graph [
          node [
            id 1
          ]
          node [
            id 2
          ]
          edge [
            source 1
            target 2
          ]
        ]
        """));

    assertEquals(2, attributeService.read(gml, GmlFormat.INSTANCE, "node_count", Integer.class));
    assertEquals(1, attributeService.read(gml, GmlFormat.INSTANCE, "edge_count", Integer.class));
  }

  @Test
  void parsesViaFormatInstance() throws FormatException {
    var gml = GmlFormat.INSTANCE.parse("""
        graph [
          node [
            id 1
            label "one"
          ]
        ]
        """);

    assertEquals(1, gml.parse().nodes().size());
    assertEquals("one", gml.parse().nodes().getFirst().label());
  }

  @Test
  void documentToStringReturnsSourceWithoutParsing() {
    var gmlString = "graph [ node [ id 1 ] ]";
    var gml = Document.of(gmlString, Gml::fromString);

    assertEquals(gmlString, gml.toString());
  }

  @Test
  void documentParsingIsDeferredUntilParse() {
    var gml = Document.of("this is not gml", Gml::fromString);

    assertThrows(ParseException.class, gml::parse);
  }
}