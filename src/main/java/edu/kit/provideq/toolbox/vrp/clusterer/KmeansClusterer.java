package edu.kit.provideq.toolbox.vrp.clusterer;

import static edu.kit.provideq.toolbox.SolutionStatus.INVALID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.setting.IntegerSetting;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.meta.setting.Text;

@Component
public class KmeansClusterer extends VrpClusterer {
    private final ApplicationContext context;
    private final String binaryDir;
    private final String binaryName;
    private ResourceProvider resourceProvider;

    private final String CLUSTER_SETTING_NAME = "cluster";
    
    @Autowired
    public KmeansClusterer(
        @Value("${vrp.directory}") String binaryDir,
        @Value("${vrp.bin.meta-solver}") String binaryName,
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
        return "Kmeans VRP Clusterer";
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

        // Retrieve the problem directory
        String problemDirectoryPath;
        try {
            problemDirectoryPath = resourceProvider
                .getProblemDirectory(problem.type(), solution.getId())
                .getAbsolutePath();
        } catch (IOException e) {
            solution.setDebugData("Failed to retrieve problem directory.");
            solution.abort();
            return;
        }

        // solve each subproblem
        for (var subproblemEntry : processResult.output().orElse(new HashMap<>()).entrySet()) {
            var vrpSolver = subRoutinePool.<String, String>getSubRoutine(ProblemType.VRP);
            var vrpSolution = vrpSolver.apply(subproblemEntry.getValue());
            if (vrpSolution.getStatus() == INVALID) {
                solution.setDebugData(vrpSolution.getDebugData());
                solution.abort();
                return;
            }

            var fileName = subproblemEntry.getKey().getFileName().toString().replace(".vrp", ".sol");

            var solutionFilePath = Path.of(problemDirectoryPath, ".vrp", fileName);

            try {
				Files.writeString(solutionFilePath, vrpSolution.getSolutionData());
			} catch (IOException e) {
				solution.setDebugData("Failed to write solution file. Path: " + solutionFilePath.toString());
                solution.abort();
                return;
			}
        }

        // combine the solution paths
        var combineProcessRunner = context.getBean(
          BinaryProcessRunner.class,
          binaryDir,
          binaryName,
          "solve",
          new String[] { "%1$s", "cluster-from-file", "solution-from-file", "--build-dir", "%3$s/.vrp", "--solution-dir", "%3$s/.vrp", "--cluster-file", "%3$s/.vrp/problem.map"}
        )
        .problemFileName("problem.vrp")
        .solutionFileName("problem.sol")
        .run(problem.type(), solution.getId(), problem.problemData());
        
        
      if (!combineProcessRunner.success()) {
        solution.setDebugData(combineProcessRunner.errorOutput().orElse("Unknown error occurred."));
        solution.abort();
        return;
      }
  
      solution.setSolutionData(combineProcessRunner.output().orElse("Empty Solution"));
      solution.complete();
    }

}
