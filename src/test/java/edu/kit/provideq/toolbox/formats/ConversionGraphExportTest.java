package edu.kit.provideq.toolbox.formats;

import static edu.kit.provideq.toolbox.demonstrators.DemonstratorConfiguration.DEMONSTRATOR;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.graph.ConversionGraphExporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import edu.kit.provideq.formats.core.converters.graph.LabeledEdge;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
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
  private ProblemManagerProvider managerProvider;

  /**
   * Returns one edge per registered solver, labeled with the solver's name.
   */
  public List<LabeledEdge> getEdges() {
    return managerProvider.getProblemManagers().stream()
            .filter(manager -> !manager.getType().equals(DEMONSTRATOR))
            .flatMap(manager -> solverEdges(manager).stream())
            .toList();
  }

  /**
   * Returns a list of LabeledEdge objects representing the solvers for the given problem manager.
   */
  private static List<LabeledEdge> solverEdges(ProblemManager<?, ?> manager) {
    var inputFamily = manager.getType().getInputFamily();
    var resultFamily = manager.getType().getResultFamily();

    return manager.getSolvers().stream()
            .map(solver -> new LabeledEdge(
                    inputFamily, resultFamily, solver.getName()))
            .toList();
  }

  @Test
  void exportConversionGraphToMarkdown() throws IOException {
    var markdown = new ConversionGraphExporter()
        .toMarkdown(conversionRegistry, getEdges());

    var outputFile = Path.of("format-graph.md");
    Files.writeString(outputFile, markdown);

    assertTrue(Files.exists(outputFile));
    assertTrue(markdown.contains("```mermaid"));
  }
}