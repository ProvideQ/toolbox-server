package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.qubo.solvers.QiskitQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QuboSolver;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for QUBO problems.
 */
@Component
public class QuboMetaSolver extends MetaSolver<String, String, QuboSolver> {
  private final String examplesDirectoryPath;
  private final ResourceProvider resourceProvider;


  @Autowired
  public QuboMetaSolver(
      @Value("${examples.directory.qubo}") String examplesDirectoryPath,
      ResourceProvider resourceProvider,
      QiskitQuboSolver qiskitQuboSolver) {
    super(ProblemType.QUBO, qiskitQuboSolver);
    this.examplesDirectoryPath = examplesDirectoryPath;
    this.resourceProvider = resourceProvider;
  }

  @Override
  public List<String> getExampleProblems() {
    try {
      var problemStream = Objects.requireNonNull(
          getClass().getResourceAsStream("linear-problem.txt"),
          "linear-problem example for QUBO is unavailable!"
      );
      return List.of(resourceProvider.readStream(problemStream));
    } catch (Exception e) {
      throw new RuntimeException("Could not load example problems", e);
    }
  }
}
