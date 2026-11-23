package edu.kit.provideq.toolbox.meta.guard;

import java.util.Optional;

@FunctionalInterface
public interface SolutionGuard<ProblemT, ResultT> {
  Optional<String> findViolation(ProblemT problem, ResultT solution);
}
