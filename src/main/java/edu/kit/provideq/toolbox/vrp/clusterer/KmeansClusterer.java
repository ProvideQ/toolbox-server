package edu.kit.provideq.toolbox.vrp.clusterer;


import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.process.ProcessResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@SuppressWarnings("checkstyle:CommentsIndentation")
@Component
public class KmeansClusterer extends VrpClusterer {

  private static final String CLUSTER_SETTING_NAME = "kmeans-cluster-number";

  @Autowired
  public KmeansClusterer(@Value("${vrp.directory}") String binaryDir,
                         @Value("${vrp.bin.meta-solver}") String binaryName,
                         ApplicationContext context) {
    super(binaryDir, binaryName, context);
  }

  @Override
  public String getName() {
    return "Kmeans VRP Clusterer (Classical)";
  }


//  @Override
//  public List<MetaSolverSetting> getSettings() {
//    return List.of(
//        new IntegerSetting(CLUSTER_SETTING_NAME, "Number of Kmeans Cluster (default: 3)", 3));
//  }

  @Override
  public Mono<Solution<String>> solve(String input, SubRoutineResolver resolver) {
    //TODO: add setting again once architecture allows it

//    int clusterNumber = settings.stream()
//        .filter(setting -> setting.name.equals(CLUSTER_SETTING_NAME))
//        .map(setting -> (IntegerSetting) setting)
//        .findFirst()
//        .map(setting -> setting.getNumber())
//        .orElse(3);

    int clusterNumber = 3; //TODO: remove later

    var solution = new Solution<String>();

    // cluster with kmeans
    ProcessResult<HashMap<Path, String>> processResult =
        context.getBean(BinaryProcessRunner.class, binaryDir, binaryName, "partial",
                new String[] {"cluster", "%1$s", "kmeans", "--build-dir", "%3$s/.vrp",
                    "--cluster-number", String.valueOf(clusterNumber)})
            .problemFileName("problem.vrp")
            .run(getProblemType(), solution.getId(), input,
                new MultiFileProcessResultReader("./.vrp/problem_*.vrp"));

    if (processResult.output().isEmpty() || !processResult.success()) {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.abort();
      return Mono.just(solution);
    }

    var mapOfClusters = processResult.output().get();

    // Retrieve the problem directory
    String problemDirectoryPath;
    try {
      problemDirectoryPath =
          resourceProvider.getProblemDirectory(getProblemType(), solution.getId())
              .getAbsolutePath();
    } catch (IOException e) {
      solution.setDebugData("Failed to retrieve problem directory.");
      solution.fail();
      return Mono.just(solution);
    }

    //solve VRP clusters:
    return Flux.fromIterable(mapOfClusters.entrySet())
        .flatMap(cluster -> resolver.runSubRoutine(VRP_SUBROUTINE, cluster.getValue())
            .map(clusterSolution -> Tuples.of(cluster.getKey(), clusterSolution)))
        .collectMap(Tuple2::getT1, Tuple2::getT2)
        .publishOn(Schedulers.boundedElastic())
        .map(clusterSolutionMap -> {
          // write solutions of the clusters into files:
          System.out.println("ENTRYMAP SIZE: " + clusterSolutionMap.entrySet().size());

          for (var entry : clusterSolutionMap.entrySet()) {
            String fileName = entry.getKey().getFileName().toString().replace(".vrp", ".sol");
            System.out.println("FileName: " + fileName);
            Path solutionFilePath = Path.of(problemDirectoryPath, ".vrp", fileName);
            System.out.println("SolutionFilePath: " + solutionFilePath);
            var clusterSolution = entry.getValue();
            System.out.println("Cluster Solution: " + entry.getValue());
            try {
              Files.writeString(solutionFilePath, clusterSolution.getSolutionData());
            } catch (IOException e) {
              solution.setDebugData("Failed to write solution file. Path: " + solutionFilePath);
              solution.fail();
              return solution;
            }
          }

          // use the combineProcessRunner to combine the solution from the written files into one solution
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

          System.out.println("Solution From Combine Process Runner:");
          System.out.println(combineProcessRunner.output());

          if (combineProcessRunner.output().isEmpty() || !combineProcessRunner.success()) {
            solution.setDebugData(
                combineProcessRunner.errorOutput().orElse("Unknown error occurred."));
            solution.fail();
            return solution;
          }

          solution.complete();
          return solution;
        });
  }
}
