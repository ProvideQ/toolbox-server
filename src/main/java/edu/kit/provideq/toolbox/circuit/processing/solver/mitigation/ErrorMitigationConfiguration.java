package edu.kit.provideq.toolbox.circuit.processing.solver.mitigation;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.circuit.processing.results.Result;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorMitigationConfiguration {
  public static final ProblemType<String, Result> MITIGATION_CONFIG = new ProblemType<>(
      "circuit-processing-mitigation",
      "A quantum circuit error mitigation problem that applies error mitigation techniques to "
          + "a given QASM circuit.",
      String.class,
      Result.class
  );

  @Bean
  ProblemManager<String, Result> getMitigationProblemManager(
      ResourceProvider provider,
      ErrorMitigationSolver errorMitigationSolver
  ) {
    return new ProblemManager<>(
        MITIGATION_CONFIG,
        Set.of(errorMitigationSolver),
        loadExampleProblems(provider)
    );
  }

  private Set<Problem<String, Result>> loadExampleProblems(ResourceProvider provider) {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("../../bell-state.qasm"), "Problem bell-state.qasm not found");
      var problem = new Problem<>(MITIGATION_CONFIG);
      problem.setInput(provider.readStream(problemStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(MITIGATION_CONFIG, e);
    }
  }
}
