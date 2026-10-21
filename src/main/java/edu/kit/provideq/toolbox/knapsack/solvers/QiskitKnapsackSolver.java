package edu.kit.provideq.toolbox.knapsack.solvers;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.knapsack.KnapsackConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.DefaultProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * {@link KnapsackConfiguration#KNAPSACK} solver using a Qiskit implementation.
 */
@Component
public class QiskitKnapsackSolver extends KnapsackSolver {
  private static final SubRoutineDefinition<String, String> QUBO_SUBROUTINE =
      new SubRoutineDefinition<>(
              QuboConfiguration.QUBO,
          "How should the QUBO be solved?",
          true
      );

  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;
  private ResourceProvider resourceProvider;

  @Autowired
  public QiskitKnapsackSolver(
      @Value("${path.qiskit.knapsack}") String scriptPath,
      @Value("${venv.qiskit.knapsack}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Autowired
  public void setResourceProvider(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  @Override
  public String getName() {
    return "Qiskit Knapsack";
  }

  @Override
  public String getDescription() {
    return "Solves the knapsack problem using Qiskit with QAOA.";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(QUBO_SUBROUTINE);
  }

  private String getProblemDirectory(Solution<String> solutionObject) {
    // write solution to current problem directory
    String problemDirectoryPath = null;
    try {
      problemDirectoryPath = resourceProvider
              .getProblemDirectory(getProblemType(), solutionObject.getId())
              .getAbsolutePath();
    } catch (IOException e) {
      solutionObject.setDebugData("Failed to retrieve problem directory.");
      solutionObject.abort();
    }
    return problemDirectoryPath;
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH,
            "--transform-only"
        )
        .writeInputFile(input)
        .readOutputFile("problem.lp")
        .run(getProblemType(), solution.getId());

    Optional<String> output = processResult.output();
    if (!processResult.success() || output.isEmpty()) {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.abort();
      return Mono.just(solution);
    }

    String problemDirectoryPath = getProblemDirectory(solution);
    if (problemDirectoryPath == null) {
      solution.setDebugData("Unable to solve Problem, the problemDirectoryPath is null.");
      solution.abort();
      return Mono.just(solution);
    }
    Path quboSolutionFilePath = Path.of(problemDirectoryPath, "problem.bin");

    String finalInput = input;
    return subRoutineResolver.runSubRoutine(QUBO_SUBROUTINE, output.get())
            .publishOn(Schedulers.boundedElastic()) //avoids block from Files.writeString() in try/catch
            .map(subRoutineSolution -> {
              if (subRoutineSolution.getSolutionData() == null
                      || subRoutineSolution.getSolutionData().isEmpty()) {
                solution.setDebugData("Unable to process at least one Subroutine, "
                        + "because its SolutionData does not exist");
                solution.abort();
                return solution;
              }

              try {
                Files.writeString(quboSolutionFilePath, subRoutineSolution.getSolutionData());
              } catch (IOException e) {
                solution.setDebugData(
                        "Failed to write qubo solution file with path: " + quboSolutionFilePath);
                solution.abort();
                return solution;
              }

              var processRetransformResult = context
                      .getBean(PythonProcessRunner.class, scriptPath, venv)
                      .withArguments(
                              ProcessRunner.INPUT_FILE_PATH,
                              ProcessRunner.OUTPUT_FILE_PATH,
                              "--qubo-solution", quboSolutionFilePath.toString()
                      )
                      .writeInputFile(finalInput)
                      .readOutputFile()
                      .run(getProblemType(), solution.getId());

              if (!processRetransformResult.success()) {
                solution.setDebugData(
                        processRetransformResult.errorOutput().orElse("Unable to retransform result."));
                solution.abort();
                return solution;
              }

              solution.setSolutionData(processRetransformResult.output().orElse("Empty Solution"));
              solution.complete();

              return solution;
            });
  }
}
