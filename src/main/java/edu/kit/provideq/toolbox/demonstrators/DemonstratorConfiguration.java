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
   * A demonstrator is a broad group of processes that
   * demonstrate certain aspects of quantum computing.
   */
  public static final ProblemType<String, String> DEMONSTRATOR = new ProblemType<>(
      "demonstrator",
      String.class,
      String.class
  );

  @Bean
  ProblemManager<String, String> getDemonstratorManager(
      CplexMipDemonstrator cplexMipDemonstrator,
      MoleculeEnergySimulator moleculeEnergySimulator
  ) {
    return new ProblemManager<>(DEMONSTRATOR,
        Set.of(cplexMipDemonstrator, moleculeEnergySimulator),
        Set.of(new Problem<>(DEMONSTRATOR)));
  }
}
