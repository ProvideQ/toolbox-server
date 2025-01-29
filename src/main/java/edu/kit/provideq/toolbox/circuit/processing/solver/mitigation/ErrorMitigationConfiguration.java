package edu.kit.provideq.toolbox.circuit.processing.solver.mitigation;

import edu.kit.provideq.toolbox.circuit.processing.solver.executor.ExecutionResult;
import edu.kit.provideq.toolbox.circuit.processing.solver.executor.ExecutionSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ErrorMitigationConfiguration {
  public static final ProblemType<String, String> MITIGATION_CONFIG = new ProblemType<>(
      "circuit-processing-mitigation",
      String.class,
      String.class,
      null
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
