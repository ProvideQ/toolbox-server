package edu.kit.provideq.toolbox.vrp.clusterer;


import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.basic.IntegerSetting;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.vrp.VrpConfiguration;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KmeansClusterer extends VrpClusterer {
  private static final String SETTING_CLUSTER_NUMBER = "Cluster Number";
  private static final int DEFAULT_CLUSTER_NUMBER = 3;

  @Autowired
  public KmeansClusterer(@Value("${custom.berger-vrp.directory}") String binaryDir,
                         @Value("${custom.berger-vrp.solver}") String binaryName,
                         ApplicationContext context) {
    super(binaryDir, binaryName, context);
  }

  protected static final SubRoutineDefinition<String, String> VRP_SUBROUTINE =
      new SubRoutineDefinition<>(
          VrpConfiguration.VRP,
          "Solve a VRP problem"
      );

  @Override
  public String getName() {
    return "K-means Clustering (VRP -> Set of VRP)";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(VRP_SUBROUTINE);
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new IntegerSetting(SETTING_CLUSTER_NUMBER, "The number of clusters to create", 1, 1000, DEFAULT_CLUSTER_NUMBER)
    );
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver resolver,
      SolvingProperties properties) {

    // for now, set the cluster number to three. Our architecture currently does not allow settings.
    // called in python script via "kmeans-cluster-number"
    int clusterNumber = properties.<IntegerSetting>getSetting(SETTING_CLUSTER_NUMBER)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_CLUSTER_NUMBER);

    if (clusterNumber < 1) {
      throw new IllegalArgumentException("Cluster number must be greater than 0");
    }

    var solution = new Solution<>(this);

    // cluster with kmeans
    ProcessResult<HashMap<Path, String>> processResult =
        context.getBean(BinaryProcessRunner.class, binaryDir, binaryName, "partial",
                new String[] {"cluster", "%1$s", "kmeans", "--build-dir", "%3$s/.vrp",
                    "--cluster-number", String.valueOf(clusterNumber)})
            .problemFileName("problem.vrp")
            .run(getProblemType(), solution.getId(), input,
                new MultiFileProcessResultReader("./.vrp/problem_*.vrp"));

    return getSolutionForCluster(input, solution, processResult, resolver, VRP_SUBROUTINE);
  }
}
