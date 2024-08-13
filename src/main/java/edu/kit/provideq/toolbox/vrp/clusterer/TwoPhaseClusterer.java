package edu.kit.provideq.toolbox.vrp.clusterer;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.tsp.TspConfiguration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TwoPhaseClusterer extends VrpClusterer {

  protected static final SubRoutineDefinition<String, String> TSP_SUBROUTINE =
      new SubRoutineDefinition<>(
          TspConfiguration.TSP,
          "Solve a TSP problem"
      );

  @Autowired
  public TwoPhaseClusterer(
      @Value("${custom.berger-vrp.directory}") String binaryDir,
      @Value("${custom.berger-vrp.solver}") String binaryName,
      ApplicationContext context) {
    super(binaryDir, binaryName, context);
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(TSP_SUBROUTINE);
  }

  @Override
  public String getName() {
    return "Two Phase Clustering (VRP -> Set of TSP)";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver resolver
  ) {

    var solution = new Solution<String>();

    // cluster with tsp/two-phase clustering
    var processResult = context.getBean(
            BinaryProcessRunner.class,
            binaryDir,
            binaryName,
            "partial",
            new String[] {"cluster", "%1$s", "tsp", "--build-dir", "%3$s/.vrp"}
        )
        .problemFileName("problem.vrp")
        .run(getProblemType(), solution.getId(), input,
            new MultiFileProcessResultReader("./.vrp/problem_*.vrp"));

    return getSolutionForCluster(input, solution, processResult, resolver, TSP_SUBROUTINE);
  }
}
