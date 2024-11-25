package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.BoundType;
import edu.kit.provideq.toolbox.BoundWithInfo;

public record BoundDto(Float bound, BoundType boundType, long executionTime) {
  public BoundDto(BoundWithInfo boundWithInfo) {
    this(
        boundWithInfo.bound().value(),
        boundWithInfo.bound().boundType(),
        boundWithInfo.executionTime());
  }
}
