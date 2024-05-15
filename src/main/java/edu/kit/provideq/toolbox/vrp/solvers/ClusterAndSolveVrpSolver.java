package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.vrp.clusterer.ClusterVrpConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

import static edu.kit.provideq.toolbox.SolutionStatus.INVALID;

@Component
public class ClusterAndSolveVrpSolver extends VrpSolver {
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

  private static final SubRoutineDefinition<String, String> CLUSTER_SUBROUTINE =
          new SubRoutineDefinition<>(
                  ClusterVrpConfiguration.CLUSTER_VRP,
                  "Creates a cluster of multiple vehicle routing problems"
          );

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(CLUSTER_SUBROUTINE);
  }

  @Override
  public Mono<Solution<String>> solve(
          String input,
          SubRoutineResolver resolver
  ) {
    return resolver.runSubRoutine(CLUSTER_SUBROUTINE, input)
            .map(clusterSolution -> {
              var solution = new Solution<String>();
              //retreive clusters
              //TODO ...
              solution.setSolutionData("currently not implemented");
              solution.setDebugData("currently not implemented");

              //solve every cluster with a vrp solver
              //TODO ...

              return solution;
            });
  }
}
