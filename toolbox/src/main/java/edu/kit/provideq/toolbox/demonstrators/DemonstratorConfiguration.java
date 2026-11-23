package edu.kit.provideq.toolbox.demonstrators;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.toolbox.demonstrators.mip.CplexMipDemonstrator;
import edu.kit.provideq.toolbox.demonstrators.moleculeenergy.MoleculeEnergySimulator;
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
  public static final ProblemType<DemonstratorFamily, DemonstratorReportFamily> DEMONSTRATOR =
      new ProblemType<>(
      "demonstrator",
      "A demonstrator is a broad group of processes that demonstrate "
          + "certain aspects of quantum computing.",
      DemonstratorFamily.INSTANCE,
      DemonstratorReportFamily.INSTANCE
  );

  @Bean
  DemonstratorFamily registerDemonstratorFamily(ConversionRegistry registry) {
    registry.registerFormat(DemonstratorFamily.INSTANCE);
    return DemonstratorFamily.INSTANCE;
  }

  @Bean
  DemonstratorReportFamily registerDemonstratorReportFamily(ConversionRegistry registry) {
    registry.registerFormat(DemonstratorReportFamily.INSTANCE);
    return DemonstratorReportFamily.INSTANCE;
  }

  @Bean
  ProblemManager<DemonstratorFamily, DemonstratorReportFamily> getDemonstratorManager(
      CplexMipDemonstrator cplexMipDemonstrator,
      MoleculeEnergySimulator moleculeEnergySimulator
  ) {
    var manager = new ProblemManager<>(DEMONSTRATOR, Set.of(new Problem<>(DEMONSTRATOR)));
    manager.registerSolvers(cplexMipDemonstrator, moleculeEnergySimulator);
    return manager;
  }
}
