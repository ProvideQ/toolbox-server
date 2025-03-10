package edu.kit.provideq.toolbox.circuit.processing.solver.mitigation;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorMitigationConfiguration {
  public static final ProblemType<String, String> MITIGATION_CONFIG = new ProblemType<>(
      "circuit-processing-mitigation",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getMitigationProblemManager(
      ErrorMitigationSolver errorMitigationSolver
  ) {
    return new ProblemManager<>(
        MITIGATION_CONFIG,
        Set.of(
            errorMitigationSolver
        ),
        Set.of(new Problem<>(MITIGATION_CONFIG))
    );
  }
}
