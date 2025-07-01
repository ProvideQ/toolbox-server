package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.vrp.clusterer.VrpClustererConfiguration;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ClusterAndSolveVrpSolver extends VrpSolver {
  private static final SubRoutineDefinition<String, String> CLUSTER_SUBROUTINE =
      new SubRoutineDefinition<>(
          VrpClustererConfiguration.CLUSTER_VRP,
          "Creates a cluster of multiple vehicle routing problems",
          true
      );

  @Override
  public String getName() {
    return "Cluster before Solving";
  }

  @Override
  public String getDescription() {
    return "Solves a vehicle routing problem by clustering it into multiple smaller problems "
        + "first.";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(CLUSTER_SUBROUTINE);
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver resolver,
      SolvingProperties properties
  ) {
    return resolver.runSubRoutine(CLUSTER_SUBROUTINE, input);
  }
}
