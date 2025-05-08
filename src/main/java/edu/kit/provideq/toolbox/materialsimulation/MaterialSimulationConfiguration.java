package edu.kit.provideq.toolbox.materialsimulation;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
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
  public static final ProblemType<String, String> MATERIAL_SIMULATION = new ProblemType<>(
      "materialsimulation",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getMaterialSimulationManager(
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        MATERIAL_SIMULATION,
        Set.of(),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("h2.txt"),
          "H2 example for Material Simulation is unavailable!"
      );
      var problem = new Problem<>(MATERIAL_SIMULATION);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(MATERIAL_SIMULATION, e);
    }
  }


}
