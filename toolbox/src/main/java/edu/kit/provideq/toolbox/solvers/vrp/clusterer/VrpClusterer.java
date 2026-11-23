package edu.kit.provideq.toolbox.solvers.vrp.clusterer;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.formats.tsplib.VRPSolutionFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.DefaultProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.solvers.vrp.VrpFamily;
import edu.kit.provideq.toolbox.solvers.vrp.VrpSolutionFamily;
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
public abstract class VrpClusterer implements ProblemSolver<VrpFamily, VrpSolutionFamily> {

  protected final ApplicationContext context;
  protected final String binaryPath;
  protected ResourceProvider resourceProvider;

  protected VrpClusterer(
      String binaryPath,
      ApplicationContext context
  ) {
    this.binaryPath = binaryPath;
    this.context = context;
  }

  @Override
  public ProblemType<VrpFamily, VrpSolutionFamily> getProblemType() {
    return VrpClustererConfiguration.CLUSTER_VRP;
  }

  @Autowired
  public void setResourceProvider(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  protected <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      Mono<Solution<VrpSolutionFamily>> getSolutionForCluster(
      String input,
      Solution<VrpSolutionFamily> solution,
      ProcessResult<HashMap<Path, String>> processResult,
      SubRoutineResolver resolver,
      SubRoutineDefinition<SubInT, SubOutT> definition,
      Format<String> clusterFormat,
      ConversionService conversionService) {
    if (processResult.output().isEmpty() || !processResult.success()) {
      solution.setDebugData(processResult.errorOutput()
          .orElse("Unknown Error Occured: Map of Cluster could not be retrieved."));
      solution.abort();
      return Mono.just(solution);
    }

    var mapOfClusters = processResult.output().get();

    //solve TSP clusters:
    return Flux.fromIterable(mapOfClusters.entrySet())
        .flatMap(cluster -> resolver.runSubRoutine(definition,
            FormattedValue.of(cluster.getValue(), clusterFormat), conversionService)
            .map(clusterSolution -> Tuples.of(cluster.getKey(), clusterSolution)))
        .<Path, Solution<?>>collectMap(Tuple2::getT1, Tuple2::getT2)
        .publishOn(Schedulers.boundedElastic())
        .map(clusterSolutionMap -> solveCluster(input, solution, clusterSolutionMap));
  }

  protected Solution<VrpSolutionFamily> solveCluster(
      String input,
      Solution<VrpSolutionFamily> solution,
      Map<Path, ? extends Solution<?>> clusterSolutionMap) {
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
      String fileName = entry.getKey().getFileName().toString().replace(".vrp", ".sol");
      Path solutionFilePath = Path.of(problemDirectoryPath, ".vrp", fileName);
      var clusterSolution = entry.getValue();
      try {
        Files.writeString(solutionFilePath, clusterSolution.getSolutionDataAsText());
      } catch (IOException e) {
        solution.setDebugData("Failed to write solution file. Path: " + solutionFilePath);
        solution.fail();
        return solution;
      }
    }

    // use the combineProcessRunner to combine the solution from the written files
    // into one solution of the original problem
    var combineProcessRunner = context
        .getBean(DefaultProcessRunner.class)
        .withArguments(
            binaryPath,
            "solve",
            ProcessRunner.INPUT_FILE_PATH,
            "cluster-from-file",
            "solution-from-file",
            "--build-dir", ProcessRunner.PROBLEM_DIRECTORY_PATH + "/.vrp",
            "--solution-dir", ProcessRunner.PROBLEM_DIRECTORY_PATH + "/.vrp",
            "--cluster-file", ProcessRunner.PROBLEM_DIRECTORY_PATH + "/.vrp/problem.map"
        )
        .writeInputFile(input, "problem.vrp")
        .readOutputFile("problem.sol")
        .run(getProblemType(), solution.getId());

    var result = combineProcessRunner.output();

    if (result.isEmpty() || !combineProcessRunner.success()) {
      solution.setDebugData(
          combineProcessRunner.errorOutput().orElse("Unknown Error: Could not combine clusters."));
      solution.fail();
      return solution;
    }

    solution.setSolutionData(result.get(), VRPSolutionFormat.INSTANCE);
    solution.complete();
    return solution;
  }
}
