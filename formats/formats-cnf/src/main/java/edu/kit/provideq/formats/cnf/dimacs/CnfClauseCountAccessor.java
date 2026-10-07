package edu.kit.provideq.formats.cnf.dimacs;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.PropertyAccessor;

/**
 * Reads the clause count property from DIMACS CNF formulas.
 */
public record CnfClauseCountAccessor() implements PropertyAccessor<DimacsCnf, Integer> {
  /**
   * Name of the property this accessor reads.
   */
  public static final String PROPERTY = "clauseCount";

  @Override
  public Format<DimacsCnf> getFormat() {
    return DimacsCnfFormat.INSTANCE;
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
  public Integer read(DimacsCnf value) {
    return value.getOrClauses().size();
  }
}
