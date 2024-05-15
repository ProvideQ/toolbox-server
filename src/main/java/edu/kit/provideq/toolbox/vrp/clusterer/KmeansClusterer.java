package edu.kit.provideq.toolbox.vrp.clusterer;


import java.util.List;

import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.setting.IntegerSetting;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import reactor.core.publisher.Mono;

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
        var processResult = context.getBean(
          BinaryProcessRunner.class,
          binaryDir,
          binaryName,
          "partial",
          new String[] { "cluster", "%1$s", "kmeans", "--build-dir", "%3$s/.vrp", "--cluster-number", String.valueOf(clusterNumber)}
        )
        .problemFileName("problem.vrp")
        .run(getProblemType(), solution.getId(), input, new MultiFileProcessResultReader("./.vrp/problem_*.vrp"));
        
        if (!processResult.success()) {
          solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
          solution.abort();
          return Mono.just(solution);
        }

        // solve the clusters
        return solveClusters(solution, resolver, processResult);
    }

}
