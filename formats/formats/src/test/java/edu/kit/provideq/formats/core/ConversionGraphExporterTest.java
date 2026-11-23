package edu.kit.provideq.formats.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.FuncConverter;
import edu.kit.provideq.formats.core.converters.graph.ConversionGraphExporter;
import edu.kit.provideq.formats.core.converters.graph.LabeledEdge;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for the conversion graph exporter.
 */
class ConversionGraphExporterTest {
  private final ConversionGraphExporter exporter = new ConversionGraphExporter();

  @Test
  void mermaidContainsNodesAndEdges() {
    var registry = new ConversionRegistry();
    var a = new TestFormat("a");
    var b = new TestFormat("b");
    registry.registerConverter(new FuncConverter<>(
        a, b, value -> value));

    var mermaid = exporter.toMermaid(registry);

    assertTrue(mermaid.startsWith("graph LR"));
    assertTrue(mermaid.contains("a[\"a\"]"));
    assertTrue(mermaid.contains("b[\"b\"]"));
    assertTrue(mermaid.contains("a --> b"));
  }

  @Test
  void unlabeledEdgeForPlainConverter() {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        new TestFormat("a"), new TestFormat("b"),
        value -> value));

    var mermaid = exporter.toMermaid(registry);

    assertTrue(mermaid.contains("a --> b"));
    assertFalse(mermaid.contains("|"));
  }

  @Test
  void formatsAreGroupedByFamily() {
    var registry = new ConversionRegistry();
    var a = new TestFormat("a");
    var b = new TestFormat("b");
    var loose = new TestFormat("loose");
    registry.registerFormat(new TestFamily("fam", a, Set.of(a, b)));
    registry.registerConverter(new FuncConverter<>(
        a, b, value -> value));
    registry.registerConverter(new FuncConverter<>(
        b, loose, value -> value));

    var mermaid = exporter.toMermaid(registry);

    assertTrue(mermaid.contains("subgraph fam [\"fam\"]"));
    assertTrue(mermaid.contains("        a[\"a\"]"));
    assertTrue(mermaid.contains("        b[\"b\"]"));
    assertTrue(mermaid.contains("    end"));
    // formats outside any family are still exported
    assertTrue(mermaid.contains("loose[\"loose\"]"));
    assertTrue(mermaid.contains("b --> loose"));
  }

  @Test
  void markdownEmbedsMermaidDiagram() {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        new TestFormat("a"), new TestFormat("b"),
        value -> value));

    var markdown = exporter.toMarkdown(registry);

    assertTrue(markdown.startsWith("# Format Conversion Graph"));
    assertTrue(markdown.contains("Formats: 2, Conversions: 1"));
    assertTrue(markdown.contains("```mermaid\ngraph LR"));
    assertTrue(markdown.endsWith("```\n"));
    assertTrue(markdown.contains("a --> b"));
  }

  @Test
  void specialCharactersAreEscaped() {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        new TestFormat("a\"b"), new TestFormat("c"),
        value -> value));

    var mermaid = exporter.toMermaid(registry);

    assertTrue(mermaid.contains("a_b[\"a\\\"b\"]"));
  }

  @Test
  void extraEdgesAreDrawnAsDashedArrows() {
    var registry = new ConversionRegistry();
    var a = new TestFormat("a");
    var b = new TestFormat("b");
    var solution = new TestFormat("solution");
    registry.registerConverter(new FuncConverter<>(
        a, b, value -> value));

    var mermaid = exporter.toMermaid(registry, List.of(
        new LabeledEdge(b, solution, "MySolver")));

    assertTrue(mermaid.contains("b -.->|\"MySolver\"| solution"));
    // formats referenced only by extra edges are added as nodes
    assertTrue(mermaid.contains("solution[\"solution\"]"));
    // conversions remain solid arrows
    assertTrue(mermaid.contains("a --> b"));
  }

  @Test
  void markdownCountsExtraEdges() {
    var registry = new ConversionRegistry();
    registry.registerConverter(new FuncConverter<>(
        new TestFormat("a"), new TestFormat("b"),
        value -> value));

    var markdown = exporter.toMarkdown(registry, List.of(
        new LabeledEdge(
            registry.getFormat("a"), registry.getFormat("b"), "Solver")));

    assertTrue(markdown.contains("Formats: 2, Conversions: 1, Solvers: 1"));
    assertTrue(markdown.contains("a -.->|\"Solver\"| b"));
  }

  @Test
  void contextConverterEdgesAreDrawnAsArrowsWithLabels() {
    var registry = new ConversionRegistry();
    var a = new TestFormat("a");
    var b = new TestFormat("b");
    registry.registerContextConverter(new TestContextConverter(a, b));

    var mermaid = exporter.toMermaid(registry);

    assertTrue(mermaid.contains("a -->|\"context: String\"| b"));
  }

  @Test
  void familyEndpointsConnectSubgraphs() {
    var registry = new ConversionRegistry();
    var a = new TestFormat("a");
    var solution = new TestFormat("solution");
    var inputFamily = new TestFamily("input-fam", a, Set.of(a));
    var resultFamily = new TestFamily("result-fam", solution, Set.of(solution));
    registry.registerFormat(inputFamily);
    registry.registerFormat(resultFamily);

    var mermaid = exporter.toMermaid(registry, List.of(
        new LabeledEdge(inputFamily, resultFamily, "MySolver")));

    // the edge connects the family subgraphs, not individual format nodes
    assertTrue(mermaid.contains("subgraph input_fam [\"input-fam\"]"));
    assertTrue(mermaid.contains("subgraph result_fam [\"result-fam\"]"));
    assertTrue(mermaid.contains("input_fam -.->|\"MySolver\"| result_fam"));
    assertFalse(mermaid.contains("a -.->"));
  }

  @Test
  void mixedEndpointsAreRejected() {
    var registry = new ConversionRegistry();
    var a = new TestFormat("a");
    var family = new TestFamily("fam", a, Set.of(a));
    registry.registerFormat(family);

    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> new LabeledEdge(a, family, "Solver"));
  }

  private record TestContextConverter(Format<String> source, Format<String> target)
      implements edu.kit.provideq.formats.core.converters.ContextConverter<
          String, String, String> {
    @Override
    public Format<String> getSource() {
      return source;
    }

    @Override
    public Format<String> getTarget() {
      return target;
    }

    @Override
    public Class<String> getContextClass() {
      return String.class;
    }

    @Override
    public String convert(String value, String context) {
      return value;
    }
  }

  private record TestFamily(
      String name,
      Format<?> canonicalFormat,
      Set<Format<?>> formats
  ) implements FormatFamily {
    @Override
    public String getName() {
      return name;
    }

    @Override
    public Format<?> getCanonicalFormat() {
      return canonicalFormat;
    }

    @Override
    public Set<Format<?>> getInitialFormats() {
      return formats;
    }
  }

  private record TestFormat(String name) implements Format<String> {
    @Override
    public String getName() {
      return name;
    }

    @Override
    public Set<String> getAliases() {
      return Set.of();
    }

    @Override
    public Set<String> getFileExtensions() {
      return Set.of();
    }

    @Override
    public FormatType getType() {
      return FormatType.PROBLEM;
    }

    @Override
    public Class<String> getRepresentationClass() {
      return String.class;
    }

    @Override
    public String parse(Reader reader) {
      return null;
    }

    @Override
    public void serialize(String value, Writer writer) {
    }
  }
}