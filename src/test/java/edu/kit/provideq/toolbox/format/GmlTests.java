package edu.kit.provideq.toolbox.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.gml.Gml;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GmlTests {
  @ParameterizedTest
  @MethodSource("testToStringParameters")
  public void testToString(String gmlString) throws ConversionException {
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
}
