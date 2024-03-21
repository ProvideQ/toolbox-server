package edu.kit.provideq.toolbox.vrp.clusterer;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.setting.IntegerSetting;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;

@Component
public class KmeansClusterer extends VrpClusterer {

    private final String CLUSTER_SETTING_NAME = "cluster";
    
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

    @Override
    public List<SubRoutineDefinition> getSubRoutines() {
        return List.of(
            new SubRoutineDefinition(ProblemType.VRP,
                "How should the clusters be solved?")
        );
    }

    @Override
    public List<MetaSolverSetting> getSettings() {
        return List.of(new IntegerSetting(CLUSTER_SETTING_NAME, "Number of Kmeans Cluster (default: 3)", 3));
    }

    @Override
    public boolean canSolve(Problem<String> problem) {
        return problem.type() == ProblemType.CLUSTERABLE_VRP;
    }

    @Override
    public void solve(Problem<String> problem, Solution<String> solution, SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {

        int clusterNumber = settings.stream()
            .filter(setting -> setting.name.equals(CLUSTER_SETTING_NAME))
            .map(setting -> (IntegerSetting) setting)
            .findFirst()
            .map(setting -> setting.number)
            .orElse(3);

        // cluster with kmeans
        var processResult = context.getBean(
          BinaryProcessRunner.class,
          binaryDir,
          binaryName,
          "partial",
          new String[] { "cluster", "%1$s", "kmeans", "--build-dir", "%3$s/.vrp", "--cluster-number", String.valueOf(clusterNumber)}
        )
        .problemFileName("problem.vrp")
        .run(problem.type(), solution.getId(), problem.problemData(), new MultiFileProcessResultReader("./.vrp/problem_*.vrp"));
        
        if (!processResult.success()) {
          solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
          solution.abort();
          return;
        }

        // solve the clusters
        solveClusters(problem, solution, subRoutinePool, processResult);
    }

}
