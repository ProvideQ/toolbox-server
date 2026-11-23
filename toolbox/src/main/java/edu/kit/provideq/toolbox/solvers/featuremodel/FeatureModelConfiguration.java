package edu.kit.provideq.toolbox.solvers.featuremodel;

import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the feature model format family with the shared conversion registry.
 *
 * <p>The family is shared by all feature model problem types (e.g. the dead feature and
 * void model anomaly problems), so it is registered once here instead of in each
 * problem type's configuration.
 */
@Configuration
public class FeatureModelConfiguration {

  @Bean
  FeatureModelFamily registerFeatureModelFamily(ConversionRegistry registry) {
    registry.registerFormat(FeatureModelFamily.INSTANCE);
    return FeatureModelFamily.INSTANCE;
  }
}
