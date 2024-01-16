package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.authentication.Authentication;
import javax.annotation.Nullable;

public record SolveOptions(
    SubRoutinePool subRoutinePool,
    @Nullable Authentication authentication) {
  public SolveOptions(SubRoutinePool subRoutinePool) {
    this(subRoutinePool, null);
  }
}
