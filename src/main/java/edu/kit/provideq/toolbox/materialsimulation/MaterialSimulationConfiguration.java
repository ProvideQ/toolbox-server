package edu.kit.provideq.toolbox.materialsimulation;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.materialsimulation.solvers.QiskitMaterialSimulationSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the Quantum Material Simulation problem.
 */
@Configuration
public class MaterialSimulationConfiguration {

  /**
   * A simulation problem, computing electronic properties of a molecular system.
   */
  public static final ProblemType<MoleculeFamily, MoleculeEnergySolutionFamily>
      MATERIAL_SIMULATION =
      new ProblemType<>(
      "MaterialSimulation",
      "A simulation problem, computing electronic properties of a molecular system.",
      MoleculeFamily.INSTANCE,
      MoleculeEnergySolutionFamily.INSTANCE
  );

  @Bean
  MoleculeFamily registerMoleculeFamily(ConversionRegistry registry) {
    registry.registerFormat(MoleculeFamily.INSTANCE);
    return MoleculeFamily.INSTANCE;
  }

  @Bean
  MoleculeEnergySolutionFamily registerMoleculeEnergySolutionFamily(
      ConversionRegistry registry) {
    registry.registerFormat(MoleculeEnergySolutionFamily.INSTANCE);
    return MoleculeEnergySolutionFamily.INSTANCE;
  }

  @Bean
  ProblemManager<MoleculeFamily, MoleculeEnergySolutionFamily> getMaterialSimulationManager(
      QiskitMaterialSimulationSolver qiskitSolver,
      ResourceProvider resourceProvider
  ) {
    var manager = new ProblemManager<>(
        MATERIAL_SIMULATION, loadExampleProblems(resourceProvider));
    manager.registerSolvers(qiskitSolver);
    return manager;
  }

  private Set<Problem<MoleculeFamily, MoleculeEnergySolutionFamily>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("h2.txt"),
          "H2 example for Material Simulation is unavailable!"
      );
      var problem = new Problem<>(MATERIAL_SIMULATION);
      problem.setInput(FormattedValue.parse(
          resourceProvider.readStream(problemInputStream), XYZMoleculeFormat.INSTANCE));
      return Set.of(problem);
    } catch (IOException | ParseException e) {
      throw new MissingExampleException(MATERIAL_SIMULATION, e);
    }
  }


}