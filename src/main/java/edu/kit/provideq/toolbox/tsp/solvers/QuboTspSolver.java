package edu.kit.provideq.toolbox.tsp.solvers;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Transforms TSP Problems into QUBOs.
 */
@Component
public class QuboTspSolver extends TspSolver {
  private static final SubRoutineDefinition<String, String> QUBO_SUBROUTINE =
      new SubRoutineDefinition<>(QuboConfiguration.QUBO, "How should the QUBO be solved?");
  private final ApplicationContext context;
  private final String binaryDir;
  private final String binaryName;
  private ResourceProvider resourceProvider;


  @Autowired
  public QuboTspSolver(
      //"vrp" value is correct because this uses the VRP framework from Lucas Bergers thesis
      @Value("${custom.berger-vrp.directory}") String binaryDir,
      @Value("${custom.berger-vrp.solver}") String binaryName,
      ApplicationContext context) {
    this.binaryName = binaryName;
    this.binaryDir = binaryDir;
    this.context = context;
  }

  @Autowired
  public void setResourceProvider(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  @Override
  public String getName() {
    return "TSP to QUBO Transformation";
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
      SubRoutineResolver resolver
  ) {
    var solution = new Solution<String>();

    // change "TYPE" keyword from "TSP" to "CVRP"
    // add capacity declaration of "0" (is ignored later)
    // this is theoretically wrong, but needed for Lucas' QUBO converter to work
    String typeRegex = "(?i)\\btype\\s*:\\s*tsp\\b";
    input = input.replaceAll(typeRegex, "TYPE : CVRP\nCAPACITY : 0");

    // translate into qubo in lp-file format with rust vrp meta solver
    var processResult = context.getBean(
            BinaryProcessRunner.class,
            binaryDir,
            binaryName,
            "partial",
            new String[] {"solve", "%1$s", "simulated", "--transform-only"}
        )
        .problemFileName("problem.vrp")
        .solutionFileName("problem.lp")
        .run(getProblemType(), solution.getId(), input);

    if (!processResult.success() || processResult.output().isEmpty()) {
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
    return resolver.runSubRoutine(QUBO_SUBROUTINE, processResult.output().get())
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

          var processRetransformResult = context.getBean(
                  BinaryProcessRunner.class,
                  binaryDir,
                  binaryName,
                  "partial",
                  new String[] {"solve", "%1$s", "simulated", "--qubo-solution",
                      quboSolutionFilePath.toString()}
              )
              .problemFileName("problem.vrp")
              .solutionFileName("problem.sol")
              .run(getProblemType(), solution.getId(), finalInput);

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
