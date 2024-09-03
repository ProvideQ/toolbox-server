package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.vrp.clusterer.VrpClustererConfiguration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ClusterAndSolveVrpSolver extends VrpSolver {
  private static final SubRoutineDefinition<String, String> CLUSTER_SUBROUTINE =
      new SubRoutineDefinition<>(
          VrpClustererConfiguration.CLUSTER_VRP,
          "Creates a cluster of multiple vehicle routing problems"
      );
  private final ApplicationContext context;

  @Autowired
  public ClusterAndSolveVrpSolver(
      ApplicationContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return "Cluster before Solving";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(CLUSTER_SUBROUTINE);
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver resolver
  ) {
    return resolver.runSubRoutine(CLUSTER_SUBROUTINE, input);
  }
}
