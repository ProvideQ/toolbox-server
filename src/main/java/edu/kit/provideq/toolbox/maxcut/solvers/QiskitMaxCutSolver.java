package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.gml.Gml;
import edu.kit.provideq.toolbox.maxcut.MaxCutConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link MaxCutConfiguration#MAX_CUT} solver using a Qiskit implementation.
 */
@Component
public class QiskitMaxCutSolver extends MaxCutSolver {
  private static final String SOLUTION_LINE_PREFIX = "solution:";

  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public QiskitMaxCutSolver(
      @Value("${qiskit.script.max-cut}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Qiskit MaxCut";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    // Parse GML to add partition data to
    Gml gml;
    try {
      gml = Gml.fromString(input);
    } catch (ConversionException e) {
      solution.setDebugData("Couldn't convert problem data to GML:\n" + e);
      solution.abort();
      return Mono.just(solution);
    }

    // Run Qiskit solver via console
    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    // Return if process failed
    if (!processResult.success()) {
      return Mono.just(processResult.applyTo(solution));
    }

    // Parse solution data and add partition data to GML
    Optional<String> solutionLine = processResult.output().get()
        .lines()
        .filter(s -> s.startsWith(SOLUTION_LINE_PREFIX))
        .findFirst();

    if (solutionLine.isPresent()) {
      // Prepare solution data from python output
      String s = solutionLine.get();
      var solutionData = s
          // Remove brackets around the solution data
          .substring(SOLUTION_LINE_PREFIX.length() + 1, s.length() - 1)
          .trim()
          .split("\\.");

      // Add partition data to each node in GML
      // We're expecting that the nodes are in the same order as in the solution data
      for (int i = 0; i < solutionData.length; i++) {
        gml.getNodes().get(i).attributes().put("partition", solutionData[i].trim());
      }
    }

    solution.setSolutionData(gml.toString());
    solution.complete();
    return Mono.just(solution);
  }
}
