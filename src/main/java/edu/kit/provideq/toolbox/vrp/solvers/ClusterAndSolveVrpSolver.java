package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.formats.VRPFormat;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.vrp.VrpFamily;
import edu.kit.provideq.toolbox.vrp.VrpSolutionFamily;
import edu.kit.provideq.toolbox.vrp.clusterer.VrpClustererConfiguration;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ClusterAndSolveVrpSolver extends VrpSolver {
  private static final SubRoutineDefinition<VrpFamily, VrpSolutionFamily> CLUSTER_SUBROUTINE =
      SubRoutineDefinition.of(
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
  public Mono<Solution<VrpSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver resolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var vrp = convertFormatted(formattedInput, VRPFormat.INSTANCE, conversionService);
    return resolver.runSubRoutine(CLUSTER_SUBROUTINE, vrp, conversionService);
  }
}