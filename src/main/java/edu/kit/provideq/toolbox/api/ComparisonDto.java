package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.BoundWithInfo;

/**
 * A DTO for a comparison between a bound and a solution.
 */
public class ComparisonDto {
  private BoundDto bound;
  private float comparison;

  /**
   * Creates a new comparison DTO out of a comparison value and a bound.
   *
   * @param comparison the comparison value, e.g., the ratio of the solution to the bound
   * @param bound a bound
   */
  public ComparisonDto(float comparison, BoundWithInfo bound) {
    this.comparison = comparison;
    this.bound = new BoundDto(bound);
  }

  public ComparisonDto() {
    this.comparison = -1;
    this.bound = null;
  }


  @Override public String toString() {
    return "Comparison{bound=%s, comparison=%f}"
        .formatted(bound, comparison);
  }

  /**
   * Gets the bound of the comparison as a BoundDto.
   *
   * @return the bound of the comparison, e.g., the best known solution or the optimal solution
   */
  public BoundDto getBound() {
    return bound;
  }

  public void setBound(BoundDto bound) {
    this.bound = bound;
  }

  public void setBound(BoundWithInfo bound) {
    this.bound = new BoundDto(bound);
  }

  public boolean hasBound() {
    return bound != null;
  }

  public void setComparison(float comparison) {
    this.comparison = comparison;
  }

  /**
   * Gets the comparison value of the comparison.
   *
   * @return the comparison value, e.g., the ratio of the solution to the bound.
   */
  public float getComparison() {
    return comparison;
  }

}
