package edu.kit.provideq.toolbox.formats;

import edu.kit.provideq.formats.core.attributes.AttributeService;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.gml.GmlFormats;
import edu.kit.provideq.formats.knapsack.KnapsackFormats;
import edu.kit.provideq.formats.lp.LpFormats;
import edu.kit.provideq.formats.qasm.QasmFormats;
import edu.kit.provideq.formats.sat.SatFormats;
import edu.kit.provideq.formats.tsplib.TsplibFormats;
import edu.kit.provideq.formats.uvl.UvlFormats;
import edu.kit.provideq.formats.xyz.XyzFormats;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the shared format conversion registry and its services as Spring beans.
 *
 * <p>All bundled format modules register their formats and converters with the shared registry.
 * Solver-specific formats may register additional formats and converters with the same registry
 * from their own configuration classes.
 */
@Configuration
public class FormatsConfiguration {
  /**
   * Creates the shared conversion registry with all bundled formats and converters.
   *
   * <p>The registry is intentionally not sealed so that solver-specific formats can register
   * themselves during application startup; registration must happen single-threaded before
   * the registry is used for lookups.
   */
  @Bean
  public ConversionRegistry conversionRegistry() {
    var registry = new ConversionRegistry();
    SatFormats.register(registry);
    GmlFormats.register(registry);
    KnapsackFormats.register(registry);
    LpFormats.register(registry);
    QasmFormats.register(registry);
    TsplibFormats.register(registry);
    UvlFormats.register(registry);
    XyzFormats.register(registry);

    return registry;
  }

  /**
   * Creates the shared conversion service over the given registry.
   *
   * @param registry the shared conversion registry
   * @return conversion service for deriving representations via the conversion graph
   */
  @Bean
  public ConversionService conversionService(ConversionRegistry registry) {
    return new ConversionService(registry);
  }

  /**
   * Creates the shared attribute service over the given registry and conversion service.
   *
   * @param registry          the shared conversion registry
   * @param conversionService the shared conversion service
   * @return attribute service for reading typed attributes from formats
   */
  @Bean
  public AttributeService attributeService(
      ConversionRegistry registry, ConversionService conversionService) {
    return new AttributeService(registry, conversionService);
  }
}
