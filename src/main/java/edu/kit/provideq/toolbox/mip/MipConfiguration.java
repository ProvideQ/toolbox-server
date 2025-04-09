package edu.kit.provideq.toolbox.mip;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.mip.solvers.CplexMip;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.mip.solvers.QuboMipSolver;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition and registration of the "Mixed Integer" problem.
 */
@Configuration
public class MipConfiguration {
  /**
   * MIP (Mixed integer Problem)
   * A mixed integer optimization problem.
   * For a given integer term,
   * find the minimal / maximal variable assignment of the term.
   */
  public static final ProblemType<String, String> MIP = new ProblemType<>(
      "mip",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getMipManager(
      CplexMip cplexMip,
      QuboMipSolver quboMipSolver,
      ResourceProvider resourceProvider
  ) {
    return new ProblemManager<>(
        MIP,
        Set.of(cplexMip, quboMipSolver),
        loadExampleProblems(resourceProvider)
    );
  }

  private Set<Problem<String, String>> loadExampleProblems(
      ResourceProvider resourceProvider) {
    try {
      var problemInputStream = Objects.requireNonNull(
          getClass().getResourceAsStream("simple.lp"),
          "mixed-integer-problem example is unavailable!"
      );
      var problem = new Problem<>(MIP);
      problem.setInput(resourceProvider.readStream(problemInputStream));
      return Set.of(problem);
    } catch (IOException e) {
      throw new MissingExampleException(MIP, e);
    }
  }
}
