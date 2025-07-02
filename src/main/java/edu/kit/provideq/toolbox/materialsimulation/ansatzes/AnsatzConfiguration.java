package edu.kit.provideq.toolbox.materialsimulation.ansatzes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.ovgu.featureide.fm.core.io.Problem;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;

@Configuration
public class AnsatzConfiguration {
  /**
   * A Configuration for Ansatzes used in Material Simulation.
   */
  public static final ProblemType<String, String> MATERIAL_SIMULATION_ANSATZ = new ProblemType<>(
    "material-simulation-ansatz",
    String.class,
    String.class
  );

  @Bean
  ProblemManager<String, String> getMaterialSimulationAnsatzesManager(
      ResourceProvider resourceProvider) {
    return new ProblemManager<>(
        MATERIAL_SIMULATION_ANSATZ,
        Set.of(new HartreeFockAnsatz(), new UCCSDAnsatz(), new UCCSDTAnsatz()),
        Set.of(new Problem<>(MATERIAL_SIMULATION_ANSATZ))
    );
  }
}
