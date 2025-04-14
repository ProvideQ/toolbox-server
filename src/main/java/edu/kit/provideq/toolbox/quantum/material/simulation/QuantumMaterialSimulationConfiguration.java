package edu.kit.provideq.toolbox.quantum.material.simulation;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Configuration;


/**
 * Definition and registration of the Quantum Material Simulation problem.
 */
@Configuration
public class QuantumMaterialSimulationConfiguration {

  /**
   * A simulation problem, computing electronic properties of a molecular system.
   */
  public static final ProblemType<String, String> QUANTUM_MATERIAL_SIMULATION = new ProblemType<>(
      "quantum-material-simulation",
      String.class,
      String.class
  );

  ProblemManager<String, String> getQuantumMaterialSimulationManager(
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        QUANTUM_MATERIAL_SIMULATION,
        Set.of(),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("empty.txt"),
          "example for Quantum Material Simulation is unavailable!"
      );
      var problem = new Problem<>(QUANTUM_MATERIAL_SIMULATION);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(QUANTUM_MATERIAL_SIMULATION, e);
    }
  }


}
