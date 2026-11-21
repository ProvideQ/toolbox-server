package edu.kit.provideq.toolbox.formats;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.graph.ConversionGraphExporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Exports the currently registered format conversion graph to a Markdown file with an
 * embedded Mermaid diagram.
 *
 * <p>Uses the shared Spring registry, which contains the bundled formats as well as all
 * formats registered by the toolbox's problem-type configurations, and includes the
 * toolbox's available solvers as dashed edges from input to output format.
 *
 * <p>Run it to (re-)generate {@code format-graph.md} in the repository root:
 * <pre>{@code ./gradlew test --tests
 *     "edu.kit.provideq.toolbox.formats.ConversionGraphExportTest"}</pre>
 */
@SpringBootTest
class ConversionGraphExportTest {
  @Autowired
  private ConversionRegistry conversionRegistry;

  @Autowired
  private SolverGraphEdges solverGraphEdges;

  @Test
  void exportConversionGraphToMarkdown() throws IOException {
    var markdown = new ConversionGraphExporter()
        .toMarkdown(conversionRegistry, solverGraphEdges.getEdges());

    var outputFile = Path.of("format-graph.md");
    Files.writeString(outputFile, markdown);

    assertTrue(Files.exists(outputFile));
    assertTrue(markdown.contains("```mermaid"));
  }
}