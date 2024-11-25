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
    public int compare(int bound, int actual) {
      return 1 - (bound / actual);
    }
  },
  /**
   * A lower bound.
   */
  LOWER {
    @Override
    public int compare(int bound, int actual) {
      return actual / bound - 1;
    }
  };

  public abstract int compare(int bound, int actual);
}
