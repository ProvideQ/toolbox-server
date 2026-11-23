package edu.kit.provideq.toolbox.unsplittablemcf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.toolbox.solvers.unsplittablemcf.formats.UmcfFormat;
import edu.kit.provideq.toolbox.solvers.unsplittablemcf.formats.UmcfInstance;
import org.junit.jupiter.api.Test;

class UmcfFormatTest {
  private static final String JSON = "{"
      + "\"config\": {\"N_PRODUCTS\": 1, \"N_TIME_STEPS\": 2, \"N_ORDERS\": 2},"
      + "\"cycle_time\": []"
      + "}";

  @Test
  void roundTripsInstance() throws Exception {
    var instance = new UmcfInstance(JSON);
    var formatted = FormattedValue.of(instance, UmcfFormat.INSTANCE);

    assertEquals(JSON, formatted.toString());
    assertEquals(JSON, FormattedValue.parse(JSON, UmcfFormat.INSTANCE).getValue().json());
  }

  @Test
  void readsConfigAttributesLazily() throws Exception {
    var formatted = FormattedValue.parse(JSON, UmcfFormat.INSTANCE);
    var readers = formatted.getFormat().getAttributeReaders();

    assertEquals("1", readers.get("products").read(formatted.getValue()));
    assertEquals("2", readers.get("time_steps").read(formatted.getValue()));
    assertEquals("2", readers.get("orders").read(formatted.getValue()));
    assertEquals("", readers.get("factories").read(formatted.getValue()));
  }
}
