package edu.kit.provideq.toolbox.vrp;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.formats.VRPFormat;
import edu.kit.provideq.toolbox.formats.VrpToTsplibConverter;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.vrp.solvers.ClusterAndSolveVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.LkhVrpSolver;
import edu.kit.provideq.toolbox.vrp.solvers.QrispVrpSolver;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VrpConfiguration {

  /**
   * A Capacitated Vehicle Routing Problem
   * Optimization Problem with the goal to find a minimal route
   * for a given set of trucks and cities with demand.
   */
  public static final ProblemType<VrpFamily, VrpSolutionFamily> VRP =
      new ProblemType<>(
      "VRP",
      "A Capacitated Vehicle Routing Problem Optimization Problem with the goal "
      + "to find a minimal route for a given set of trucks and cities with demand.",
      VrpFamily.INSTANCE,
      VrpSolutionFamily.INSTANCE
  );

  @Bean
  VrpFamily registerVrpFamily(ConversionRegistry registry) {
    registry.registerFormat(VrpFamily.INSTANCE);
    return VrpFamily.INSTANCE;
  }

  @Bean
  VrpSolutionFamily registerVrpSolutionFamily(ConversionRegistry registry) {
    registry.registerFormat(VrpSolutionFamily.INSTANCE);
    return VrpSolutionFamily.INSTANCE;
  }

  @Bean
  VrpToTsplibConverter registerVrpToTsplibConverter(ConversionRegistry registry) {
    registry.registerConverter(VrpToTsplibConverter.INSTANCE);
    return VrpToTsplibConverter.INSTANCE;
  }

  @Bean
  ProblemManager<VrpFamily, VrpSolutionFamily> getVrpManager(
      ResourceProvider resourceProvider,
      ClusterAndSolveVrpSolver clusterAndSolveVrpSolver,
      LkhVrpSolver lkhVrpSolver,
      QrispVrpSolver qrispVrpSolver) {
    var manager = new ProblemManager<>(VRP, loadExampleProblems(resourceProvider));
    manager.registerSolvers(clusterAndSolveVrpSolver, lkhVrpSolver, qrispVrpSolver);
    return manager;
  }

  private Set<Problem<VrpFamily, VrpSolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      String[] problemNames = new String[] {
          "CMT1.vrp", "SmallSample.vrp", "VerySmallSampleForGrover.vrp"
      };

      var problemSet = new HashSet<Problem<VrpFamily, VrpSolutionFamily>>();
      for (var problemName : problemNames) {
        var problemStream = Objects.requireNonNull(getClass().getResourceAsStream(problemName),
            "Problem " + problemName + " not found");
        var problem = new Problem<VrpFamily, VrpSolutionFamily>(VRP);
        problem.setInput(FormattedValue.of(

            resourceProvider.readStream(problemStream), VRPFormat.INSTANCE));
        problemSet.add(problem);
      }
      return problemSet;
    } catch (IOException e) {
      throw new MissingExampleException(VRP, e);
    }
  }
}