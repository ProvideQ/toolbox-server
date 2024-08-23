package edu.kit.provideq.toolbox.vrp.clusterer;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * A solver for SAT problems.
 */
public abstract class VrpClusterer implements ProblemSolver<String, String> {

  protected final ApplicationContext context;
  protected final String binaryDir;
  protected final String binaryName;
  protected ResourceProvider resourceProvider;

  protected VrpClusterer(
      String binaryDir,
      String binaryName,
      ApplicationContext context
  ) {
    this.binaryDir = binaryDir;
    this.binaryName = binaryName;
    this.context = context;
  }

  @Override
  public ProblemType<String, String> getProblemType() {
    return VrpClustererConfiguration.CLUSTER_VRP;
  }

  @Autowired
  public void setResourceProvider(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  protected Mono<Solution<String>> getSolutionForCluster(
      String input,
      Solution<String> solution,
      ProcessResult<HashMap<Path, String>> processResult,
      SubRoutineResolver resolver,
      SubRoutineDefinition<String, String> definition) {
    if (processResult.output().isEmpty() || !processResult.success()) {
      solution.setDebugData(processResult.errorOutput()
          .orElse("Unknown Error Occured: Map of Cluster could not be retrieved."));
      solution.abort();
      return Mono.just(solution);
    }

    var mapOfClusters = processResult.output().get();

    //solve TSP clusters:
    return Flux.fromIterable(mapOfClusters.entrySet())
        .flatMap(cluster -> resolver.runSubRoutine(definition, cluster.getValue())
            .map(clusterSolution -> Tuples.of(cluster.getKey(), clusterSolution)))
        .collectMap(Tuple2::getT1, Tuple2::getT2)
        .publishOn(Schedulers.boundedElastic())
        .map(clusterSolutionMap -> solveCluster(input, solution, clusterSolutionMap));
  }

  protected Solution<String> solveCluster(
      String input,
      Solution<String> solution,
      Map<Path, Solution<String>> clusterSolutionMap) {
    // Retrieve the problem directory
    String problemDirectoryPath;
    try {
      problemDirectoryPath =
          resourceProvider.getProblemDirectory(getProblemType(), solution.getId())
              .getAbsolutePath();
    } catch (IOException e) {
      solution.setDebugData("Failed to retrieve problem directory.");
      solution.fail();
      return solution;
    }

    // write solutions of the clusters into files:
    for (var entry : clusterSolutionMap.entrySet()) {
      // get file name of the entry and replace .vrp with .sol
      // (making clear that this file is a solution)
      String fileName = entry.getKey().getFileName().toString().replace(".vrp", ".sol");

      // get path for the solution file: [problemDirectoryPath]/.vrp/[fileName]
      Path solutionFilePath = Path.of(problemDirectoryPath, ".vrp", fileName);

      // create the solution file at the associated path:
      var clusterSolution = entry.getValue();
      try {
        Files.writeString(solutionFilePath, clusterSolution.getSolutionData());
      } catch (IOException e) {
        solution.setDebugData("Failed to write solution file. Path: " + solutionFilePath);
        solution.fail();
        return solution;
      }
    }

    // use the combineProcessRunner to combine the solution from the written files
    // into one solution of the original problem
    var combineProcessRunner =
        context.getBean(BinaryProcessRunner.class, binaryDir, binaryName, "solve",
                new String[] {"%1$s", "cluster-from-file", "solution-from-file",
                    "--build-dir",
                    "%3$s/.vrp", "--solution-dir", "%3$s/.vrp", "--cluster-file",
                    "%3$s/.vrp/problem.map"})
            .problemFileName("problem.vrp")
            .solutionFileName("problem.sol")
            .run(getProblemType(), solution.getId(), input);

    if (combineProcessRunner.output().isEmpty() || !combineProcessRunner.success()) {
      solution.setDebugData(
          combineProcessRunner.errorOutput().orElse("Unknown Error: Could not combine clusters."));
      solution.fail();
      return solution;
    }

    solution.complete();
    return solution;
  }
}
