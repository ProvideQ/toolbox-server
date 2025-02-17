package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.BoundWithInfo;
import edu.kit.provideq.toolbox.Solution;

public class ComparisonDto {
  private BoundDto bound;
  private float comparison;

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

  public BoundDto getBound() {
    return bound;
  }

  public void setBound(BoundDto bound) {
    this.bound = bound;
  }

  public void setBound(BoundWithInfo bound) {
    this.bound = new BoundDto(bound);
  }

  public void setComparison(float comparison) {
    this.comparison = comparison;
  }

  public float getComparison() {
    return comparison;
  }

}
