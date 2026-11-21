package edu.kit.provideq.toolbox.unsplittablemcf;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormatReader;
import edu.kit.provideq.formats.core.FormatType;
import edu.kit.provideq.formats.core.attributes.AttributeReader;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.exceptions.SerializationException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * The unsplittable multi commodity flow instance format.
 *
 * <p>The representation is the JSON document of the instance; the {@code config} section
 * is parsed lazily on attribute access.
 *
 * <p>Example instance (excerpt):
 * <pre>
 * {
 *     "config": {
 *         "N_PRODUCTS": 1,
 *         "N_TIME_STEPS": 2,
 *         "N_ORDERS": 2,
 *         "N_FACTORIES": 1,
 *         "MIN_CAPACITY": 10,
 *         "MAX_CAPACITY": 15,
 *         "PENALTY_DELAY": 5,
 *         "PENALTY_SLACK": 40
 *     },
 *     "cycle_time": [["v_sort0", 1.0]],
 *     "capacity": [["f0", 15.0]]
 * }
 * </pre>
 */
public final class UmcfFormat implements Format<UmcfInstance> {
  public static final UmcfFormat INSTANCE = new UmcfFormat();

  private UmcfFormat() {
  }

  @Override
  public Map<String, AttributeReader<UmcfInstance, ?>> getAttributeReaders() {
    return Map.of(
        "products", AttributeReader.of(
            String.class, instance -> instance.getConfigValue("N_PRODUCTS")),
        "time_steps", AttributeReader.of(
            String.class, instance -> instance.getConfigValue("N_TIME_STEPS")),
        "orders", AttributeReader.of(
            String.class, instance -> instance.getConfigValue("N_ORDERS")),
        "factories", AttributeReader.of(
            String.class, instance -> instance.getConfigValue("N_FACTORIES")),
        "distribution_centers", AttributeReader.of(
            String.class,
            instance -> instance.getConfigValue("N_DISTRIBUTION")));
  }

  @Override
  public String getName() {
    return "umcf";
  }

  @Override
  public Set<String> getAliases() {
    return Set.of("unsplittable-mcf");
  }

  @Override
  public Set<String> getFileExtensions() {
    return Set.of("json");
  }

  @Override
  public FormatType getType() {
    return FormatType.PROBLEM;
  }

  @Override
  public Class<UmcfInstance> getRepresentationClass() {
    return UmcfInstance.class;
  }

  @Override
  public UmcfInstance parse(Reader reader) throws ParseException {
    try {
      var json = FormatReader.readAll(reader);
      if (!json.contains("\"config\"")) {
        throw new ParseException("UMCF instance must contain a 'config' section");
      }
      return new UmcfInstance(json);
    } catch (IOException e) {
      throw new ParseException("Could not read UMCF instance", e);
    }
  }

  @Override
  public void serialize(UmcfInstance value, Writer writer) throws SerializationException {
    try {
      writer.write(value.json());
    } catch (IOException e) {
      throw new SerializationException("Could not serialize UMCF instance", e);
    }
  }
}