package edu.kit.provideq.formats.gml;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.PropertyAccessor;

/**
 * Reads the node count property from GML graphs.
 */
public record GmlNodeCountAccessor() implements PropertyAccessor<Gml, Integer> {
  /**
   * Name of the property this accessor reads.
   */
  public static final String PROPERTY = "nodeCount";

  @Override
  public Format<Gml> getFormat() {
    return GmlFormat.INSTANCE;
  }

  @Override
  public String getProperty() {
    return PROPERTY;
  }

  @Override
  public Class<Integer> getType() {
    return Integer.class;
  }

  @Override
  public Integer read(Gml value) {
    return value.nodes().size();
  }
}
