package edu.kit.provideq.toolbox.sharpsat;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.sharpsat.solvers.GanakSolver;
import edu.kit.provideq.toolbox.sharpsat.solvers.PythonBruteForceSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharpSatConfiguration {

  public static final ProblemType<String, Integer> SHARPSAT = new ProblemType<>(
      "sharpsat",
      String.class,
      Integer.class,
      null
  );


  @Bean
  ProblemManager<String, Integer> getSharpSatManager(
      PythonBruteForceSolver pythonBruteForceSolver,
      GanakSolver ganakSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        SHARPSAT,
        Set.of(pythonBruteForceSolver, ganakSolver),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, Integer>> loadExampleProblems(
      ResourceProvider resourceProvider
  ) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("simple-and.txt"),
          "Simple-And example for SAT is unavailable!"
      );
      var problem = new Problem<>(SHARPSAT);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(SHARPSAT, e);
    }
  }


}
