package edu.kit.provideq.toolbox.formats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.kit.provideq.formats.core.attributes.AttributeService;
import edu.kit.provideq.formats.core.converters.ConversionService;
import org.junit.jupiter.api.Test;

/**
 * Tests for the shared format registry configuration.
 */
class FormatsConfigurationTest {
  @Test
  void registryContainsAllBundledFormats() {
    var registry = new FormatsConfiguration().conversionRegistry();

    assertNotNull(registry.getFormat("dimacs-cnf"));
    assertNotNull(registry.getFormat("logical-expression"));
    assertNotNull(registry.getFormat("gml"));
    assertNotNull(registry.getFormat("uvl"));
  }

  @Test
  void toolboxConvertsAcrossBundledFormats() throws Exception {
    var configuration = new FormatsConfiguration();
    var registry = configuration.conversionRegistry();
    var conversionService = configuration.conversionService(registry);
    var attributeService = new AttributeService(registry, conversionService);

    var cnf = conversionService.convert("apple and banana", "logical-expression", "dimacs-cnf");

    assertNotNull(cnf);
    assertEquals(2, attributeService.read(cnf,
        registry.getFormat("dimacs-cnf"), "variable_count", Integer.class));
  }
}
