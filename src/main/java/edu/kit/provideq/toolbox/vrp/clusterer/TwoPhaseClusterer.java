package edu.kit.provideq.toolbox.vrp.clusterer;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.DefaultProcessRunner;
import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.process.ProcessRunner;
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
      @Value("${custom.binary.berger-vrp}") String binaryPath,
      ApplicationContext context) {
    super(binaryPath, context);
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
      SubRoutineResolver resolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    // cluster with tsp/two-phase clustering
    var processResult = context
        .getBean(DefaultProcessRunner.class)
        .withArguments(
            binaryPath,
            "partial",
            "cluster",
            ProcessRunner.INPUT_FILE_PATH,
            "tsp",
            "--build-dir", ProcessRunner.OUTPUT_FILE_PATH + "/.vrp"
        )
        .withInputFile(input, "problem.vrp")
        .withOutputFile(new MultiFileProcessResultReader("./.vrp/problem_*.vrp"))
        .run(getProblemType(), solution.getId());

    return getSolutionForCluster(input, solution, processResult, resolver, TSP_SUBROUTINE);
  }
}
