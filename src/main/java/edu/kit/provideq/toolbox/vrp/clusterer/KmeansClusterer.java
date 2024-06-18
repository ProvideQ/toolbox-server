package edu.kit.provideq.toolbox.vrp.clusterer;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.vrp.VrpConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.setting.IntegerSetting;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class KmeansClusterer extends VrpClusterer {

    private static final String CLUSTER_SETTING_NAME = "kmeans-cluster-number";

    @Autowired
    public KmeansClusterer(
            @Value("${vrp.directory}") String binaryDir,
            @Value("${vrp.bin.meta-solver}") String binaryName,
            ApplicationContext context) {
        super(binaryDir, binaryName, context);
    }

    @Override
    public String getName() {
        return "Kmeans VRP Clusterer (Classical)";
    }

    /*
    @Override
    public List<MetaSolverSetting> getSettings() {
        return List.of(new IntegerSetting(CLUSTER_SETTING_NAME, "Number of Kmeans Cluster (default: 3)", 3));
    }
    */

    @Override
    public Mono<Solution<String>> solve(
            String input,
            SubRoutineResolver resolver
    ) {
        //TODO: add setting again once architecture allows it
        /*
        int clusterNumber = settings.stream()
            .filter(setting -> setting.name.equals(CLUSTER_SETTING_NAME))
            .map(setting -> (IntegerSetting) setting)
            .findFirst()
            .map(setting -> setting.getNumber())
            .orElse(3);
         */
        int clusterNumber = 3; //TODO: remove later

        var solution = new Solution<String>();
        // cluster with kmeans
        ProcessResult<HashMap<Path, String>> processResult = context.getBean(
                        BinaryProcessRunner.class,
                        binaryDir,
                        binaryName,
                        "partial",
                        new String[]{"cluster", "%1$s", "kmeans", "--build-dir", "%3$s/.vrp", "--cluster-number", String.valueOf(clusterNumber)}
                )
                .problemFileName("problem.vrp")
                .run(getProblemType(), solution.getId(), input, new MultiFileProcessResultReader("./.vrp/problem_*.vrp"));

        if (processResult.output().isEmpty() || !processResult.success()) {
            solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
            solution.abort();
            return Mono.just(solution);
        }
        var mapOfClusters = processResult.output().get();

        // Retrieve the problem directory
        String problemDirectoryPath;
        try {
            problemDirectoryPath = resourceProvider
                    .getProblemDirectory(getProblemType(), solution.getId())
                    .getAbsolutePath();
        } catch (IOException e) {
            solution.setDebugData("Failed to retrieve problem directory.");
            solution.abort();
            return Mono.just(solution);
        }

        //solve VRP clusters:
        return Flux.fromIterable(mapOfClusters.values())
                .map(cluster -> {
                    Mono<Solution<String>> subroutineMono = resolver.runSubRoutine(VRP_SUBROUTINE, cluster);
                    System.out.println("Telling to solve cluster: " + cluster);
                    return subroutineMono;
                })
                .flatMap(cluster -> cluster)
                .collectList()
                .publishOn(Schedulers.boundedElastic())
                .map(vrpSolutions -> {

                    var vrpSolution = new Solution<String>();

                    //System.out.println(vrpSolutions.size());
                    for (var s : vrpSolutions) {
                        System.out.println("Solution in K-Means Clusterer: " + s);
                        var solutionFilePath = Path.of(problemDirectoryPath, ".vrp", UUID.randomUUID().toString() + ".sol");
                        try {
                            Files.writeString(solutionFilePath, s.getSolutionData());
                        } catch (IOException e) {
                            vrpSolution.setDebugData("Failed to write solution file. Path: " + solutionFilePath.toString());
                            vrpSolution.abort();
                            return vrpSolution;
                        }
                    }

                    var combineProcessRunner = context.getBean(
                                    BinaryProcessRunner.class,
                                    binaryDir,
                                    binaryName,
                                    "solve",
                                    new String[]{"%1$s", "cluster-from-file", "solution-from-file", "--build-dir", "%3$s/.vrp", "--solution-dir", "%3$s/.vrp", "--cluster-file", "%3$s/.vrp/problem.map"}
                            )
                            .problemFileName("problem.vrp")
                            .solutionFileName("problem.sol")
                            //TODO: get correct input for run()
                            .run(getProblemType(), vrpSolution.getId(), input);

                    System.out.println("Solution From Combine Process Runner:");
                    System.out.println(combineProcessRunner.output());

                    if (combineProcessRunner.output().isEmpty() || !combineProcessRunner.success()) {
                        vrpSolution.setDebugData(combineProcessRunner.errorOutput().orElse("Unknown error occurred."));
                        vrpSolution.abort();
                        return vrpSolution;
                    }

                    return vrpSolution;
                });
    }
}
