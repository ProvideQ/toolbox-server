package edu.kit.provideq.formats.core.converters.graph;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders a {@link ConversionRegistry}'s conversion graph as a diagram:
 * formats are nodes, converters are directed edges.
 *
 * <p>The Mermaid output can be embedded in Markdown files and is rendered by
 * GitHub, GitLab and most IDE Markdown previews.
 */
public class ConversionGraphExporter {
  /**
   * Renders the registry's conversion graph as a Mermaid flowchart.
   *
   * <p>Formats are grouped into one subgraph per registered format family (a format
   * belonging to multiple families is shown in the first one); formats without a family
   * are placed outside subgraphs.
   *
   * @param registry registry whose registered formats and converters are exported
   * @return Mermaid graph source
   */
  public String toMermaid(ConversionRegistry registry) {
    return toMermaid(registry, List.of());
  }

  /**
   * Renders the registry's conversion graph as a Mermaid flowchart, additionally drawing
   * the given extra edges as dashed arrows (e.g. solvers leading from an input family to
   * an output family). Extra-edge endpoints may be formats (added as nodes if unknown) or
   * format families (connected via their subgraph).
   *
   * @param registry registry whose registered formats and converters are exported
   * @param extraEdges additional edges to include in the diagram
   * @return Mermaid graph source
   */
  public String toMermaid(ConversionRegistry registry, Collection<LabeledEdge> extraEdges) {
    var formats = new LinkedHashSet<>(registry.getFormats());
    for (var edge : extraEdges) {
      if (edge.source() instanceof Format<?> source) {
        formats.add(source);
      }
      if (edge.target() instanceof Format<?> target) {
        formats.add(target);
      }
    }

    var nodeIds = new HashMap<Format<?>, String>();
    var usedIds = new HashSet<String>();
    for (var format : formats) {
      nodeIds.put(format, uniqueNodeId(format.getName(), usedIds));
    }

    var sb = new StringBuilder("graph LR\n");

    appendFamilySubgraphs(registry, formats, nodeIds, sb);
    appendEdges(registry, nodeIds, sb);
    appendContextEdges(registry, nodeIds, sb);
    appendExtraEdges(extraEdges, nodeIds, sb);

    return sb.toString();
  }

  /**
   * Renders the registry's conversion graph as a Markdown document with an embedded
   * Mermaid diagram, ready to be written to a {@code .md} file.
   *
   * @param registry registry whose registered formats and converters are exported
   * @return Markdown document containing the Mermaid diagram
   */
  public String toMarkdown(ConversionRegistry registry) {
    return toMarkdown(registry, List.of());
  }

  /**
   * Renders the registry's conversion graph as a Markdown document with an embedded
   * Mermaid diagram, additionally drawing the given extra edges as dashed arrows.
   *
   * <p>The document explains the diagram's notation: solid arrows are registered
   * converters between formats, dashed arrows are extra edges (e.g. solvers) and connect
   * either formats or format-family subgraphs.
   *
   * @param registry registry whose registered formats and converters are exported
   * @param extraEdges additional edges to include in the diagram
   * @return Markdown document containing the Mermaid diagram
   */
  public String toMarkdown(ConversionRegistry registry, Collection<LabeledEdge> extraEdges) {
    var formats = registry.getFormats();
    var converterCount = formats.stream()
        .mapToLong(format -> registry.getOutgoing(format).size())
        .sum()
        + registry.getContextConverters().size();

    return "# Format Conversion Graph\n\n"
        + "Formats: %d, Conversions: %d, Solvers: %d\n\n"
            .formatted(formats.size(), converterCount, extraEdges.size())
        + "Boxes group the formats of a format family. "
        + "Solid arrows are registered converters between formats, "
        + " where some additionally require the problem instance "
        + "as context (labeled with the context type); "
        + "dashed arrows are solvers, leading from a problem's input family "
        + "to its result family.\n\n"
        + "```mermaid\n"
        + toMermaid(registry, extraEdges)
        + "```\n";
  }

  private void appendFamilySubgraphs(
      ConversionRegistry registry,
      Set<Format<?>> formats,
      Map<Format<?>, String> nodeIds,
      StringBuilder sb) {
    var assigned = new HashSet<Format<?>>();
    for (var family : registry.getFamilies()) {
      var members = registry.getFormats(family).stream()
          .filter(formats::contains)
          .toList();
      if (members.isEmpty()) {
        continue;
      }

      sb.append("    subgraph %s [\"%s\"]\n".formatted(
          sanitizeId(family.getName()), escape(family.getName())));
      for (var format : members) {
        appendNode(format, nodeIds, sb);
        assigned.add(format);
      }
      sb.append("    end\n");
    }

    for (var format : formats) {
      if (!assigned.contains(format)) {
        appendNode(format, nodeIds, sb);
      }
    }
  }

  private void appendNode(Format<?> format, Map<Format<?>, String> nodeIds, StringBuilder sb) {
    sb.append("        %s[\"%s\"]\n".formatted(
        nodeIds.get(format), escape(format.getName())));
  }

  private void appendEdges(
      ConversionRegistry registry,
      Map<Format<?>, String> nodeIds,
      StringBuilder sb) {
    for (var format : registry.getFormats()) {
      for (var converter : registry.getOutgoing(format)) {
        sb.append("    %s --> %s\n".formatted(
            nodeIds.get(converter.getSource()), nodeIds.get(converter.getTarget())));
      }
    }
  }

  /**
   * Appends the registered context converters as arrows labeled with the required
   * context type: unlike plain converters they cannot be applied without the problem
   * instance as context.
   */
  private void appendContextEdges(
      ConversionRegistry registry,
      Map<Format<?>, String> nodeIds,
      StringBuilder sb) {
    for (var contextConverter : registry.getContextConverters()) {
      sb.append("    %s -->|\"context: %s\"| %s\n".formatted(
          nodeIds.get(contextConverter.getSource()),
          escape(contextConverter.getContextClass().getSimpleName()),
          nodeIds.get(contextConverter.getTarget())));
    }
  }

  private void appendExtraEdges(
      Collection<LabeledEdge> extraEdges,
      Map<Format<?>, String> nodeIds,
      StringBuilder sb) {
    for (var edge : extraEdges) {
      var sourceId = endpointId(edge.source(), nodeIds);
      var targetId = endpointId(edge.target(), nodeIds);
      if (edge.label().isEmpty()) {
        sb.append("    %s -.-> %s\n".formatted(sourceId, targetId));
      } else {
        sb.append("    %s -.->|\"%s\"| %s\n".formatted(
            sourceId, escape(edge.label()), targetId));
      }
    }
  }

  /**
   * Resolves an extra-edge endpoint to its Mermaid node id: formats map to their node id,
   * format families map to their subgraph id.
   */
  private String endpointId(Object endpoint, Map<Format<?>, String> nodeIds) {
    if (endpoint instanceof Format<?> format) {
      return nodeIds.get(format);
    }
    if (endpoint instanceof FormatFamily family) {
      return sanitizeId(family.getName());
    }
    throw new IllegalArgumentException(
        "Unsupported edge endpoint: " + endpoint.getClass().getName());
  }

  private String uniqueNodeId(String name, Set<String> usedIds) {
    var id = sanitizeId(name);
    var candidate = id;
    var suffix = 1;
    while (!usedIds.add(candidate)) {
      candidate = id + "_" + suffix++;
    }
    return candidate;
  }

  private String sanitizeId(String name) {
    var sanitized = name.replaceAll("[^A-Za-z0-9_]", "_");
    if (sanitized.isEmpty() || Character.isDigit(sanitized.charAt(0))) {
      sanitized = "f_" + sanitized;
    }
    return sanitized;
  }

  private String escape(String text) {
    return text.replace("\"", "\\\"");
  }
}
