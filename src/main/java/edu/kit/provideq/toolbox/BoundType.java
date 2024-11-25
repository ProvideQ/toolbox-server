package edu.kit.provideq.toolbox;

/**
 * Represents the bound type of bounds.
 */
public enum BoundType {
  /**
   * An upper bound.
   */
  UPPER {
    @Override
    public float compare(float bound, float actual) {
      return 1 - (bound / actual);
    }
  },
  /**
   * A lower bound.
   */
  LOWER {
    @Override
    public float compare(float bound, float actual) {
      return actual / bound - 1;
    }
  };

  public abstract float compare(float bound, float actual);
}
