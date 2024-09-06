package edu.kit.provideq.toolbox.demonstrators;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemonstratorConfiguration {
  /**
   * A Traveling Sales Person Problem.
   * Optimization Problem with the goal of find an optimal route
   * between a given set of connected cities.
   */
  public static final ProblemType<String, String> DEMONSTRATOR = new ProblemType<>(
      "demonstrator",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getDemonstratorManager(
      CplexMipDemonstrator cplexMipDemonstrator
  ) {
    return new ProblemManager<>(DEMONSTRATOR,
        Set.of(cplexMipDemonstrator),
        Set.of(new Problem<>(DEMONSTRATOR)));
  }
}
