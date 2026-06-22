package edu.kit.provideq.toolbox.unsplittablemcf.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import edu.kit.provideq.toolbox.unsplittablemcf.UnsplittableMcfConfiguration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * LP Export and QUBO solver for the {@link UnsplittableMcfConfiguration#UNSPLITTABLE_MCF} problem.
 * This solver exports the MCF model to LP format (QUBO reformulation) and then solves it using
 * the QUBO subroutine from the GAMS solvers.
 */
@Component
public class GamsUnsplittableMcfQuboSolver extends GamsUnsplittableMcfSolver {
  private static final SubRoutineDefinition<String, String> QUBO_SUBROUTINE =
      new SubRoutineDefinition<>(
          QuboConfiguration.QUBO,
          "How should the QUBO be solved?",
          true
      );

  private final String exportScriptPath;
  private final String plotQuboSolutionScriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public GamsUnsplittableMcfQuboSolver(
      @Value("${path.gams.unsplittable-mcf-lp-export}") String exportScriptPath,
      @Value("${path.gams.unsplittable-mcf-plot_qubo_solution}") String plotQuboSolutionScriptPath,
      @Value("${venv.gams.unsplittable-mcf}") String venv,
      ApplicationContext context) {
    this.exportScriptPath = exportScriptPath;
    this.plotQuboSolutionScriptPath = plotQuboSolutionScriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "U-MCF to QUBO Transformation";
  }

  @Override
  public String getDescription() {
    return "Solves the Unsplittable Multi Commodity Flow problem by transforming it to a "
        + "QUBO problem and solving with a QUBO solver.";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(QUBO_SUBROUTINE);
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    // Run the LP exporter
    var quboExportResult = context
        .getBean(PythonProcessRunner.class, exportScriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input, "unsplittable-mcf.json")
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    if (!quboExportResult.success()) {
      solution.setDebugData("LP export failed: "
          + quboExportResult.errorOutput().orElse("Unknown error"));
      solution.abort();
      return Mono.just(solution);
    }

    var qubo = quboExportResult.output();
    if (qubo.isEmpty()) {
      solution.setDebugData("LP export did not produce any output.");
      solution.abort();
      return Mono.just(solution);
    }

    // Call the QUBO subroutine
    return subRoutineResolver.runSubRoutine(QUBO_SUBROUTINE, qubo.get())
        .map(quboSolution -> {
          var plotQuboResult = context
              .getBean(PythonProcessRunner.class, plotQuboSolutionScriptPath, venv)
              .withArguments(
                  ProcessRunner.INPUT_FILE_PATH,
                  ProcessRunner.INPUT_FILE_PATH2,
                  ProcessRunner.OUTPUT_FILE_PATH
              )
              .writeInputFile(
                  input,
                  "unsplittable-mcf.json",
                  ProcessRunner.INPUT_FILE_PATH)
              .writeInputFile(
                  quboSolution.getSolutionData(),
                  "qubo-solution.json",
                  ProcessRunner.INPUT_FILE_PATH2)
              .readOutputFile("output.html")
              .run(getProblemType(), solution.getId());

          return plotQuboResult.applyTo(solution);
        });
  }
}
