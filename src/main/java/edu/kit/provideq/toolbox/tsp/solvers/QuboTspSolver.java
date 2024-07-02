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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
      @Value("${vrp.directory}") String binaryDir,
      //"vrp" value is correct because this uses the VRP framework from Lucas Bergers thesis
      @Value("${vrp.bin.meta-solver}") String binaryName,
      ApplicationContext context) {
    this.binaryName = binaryName;
    this.binaryDir = binaryDir;
    this.context = context;
  }

  public static boolean checkVehicleCapacity(String tsp) {
    int capacity = 0;
    int totalDemand = 0;
    boolean demandSection = false;

    Pattern capacityPattern = Pattern.compile("CAPACITY\\s*:\\s*(\\d+)");
    Pattern demandPattern = Pattern.compile("^\\d+\\s*(\\d+)");

    for (String line : tsp.split("\n")) {
      Matcher capacityMatcher = capacityPattern.matcher(line);
      if (capacityMatcher.find()) {
        capacity = Integer.parseInt(capacityMatcher.group(1));
      }
      if (line.startsWith("DEMAND_SECTION")) {
        demandSection = true;
        continue;
      }
      if (line.startsWith("EOF")) {
        demandSection = false;
      }
      if (demandSection) {
        Matcher demandMatcher = demandPattern.matcher(line);
        if (demandMatcher.find()) {
          totalDemand += Integer.parseInt(demandMatcher.group(1));
        }
      }
    }

    return totalDemand <= capacity;
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

    return resolver.runSubRoutine(QUBO_SUBROUTINE, processResult.output().get())
        .publishOn(Schedulers.boundedElastic())
        .map(subRoutineSolution -> {
          String problemDirectoryPath = getProblemDirectory(solution);
          if (problemDirectoryPath == null) {
            return solution;
          }
          Path quboSolutionFilePath = Path.of(problemDirectoryPath, "problem.bin");

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
              .run(getProblemType(), solution.getId(), input);

          if (!processRetransformResult.success()) {
            solution.setDebugData(
                processRetransformResult.errorOutput().orElse("Unknown error occurred."));
            solution.abort();
            return solution;
          }

          solution.setSolutionData(processRetransformResult.output().orElse("Empty Solution"));
          solution.complete();

          return solution;
        });
  }
}
