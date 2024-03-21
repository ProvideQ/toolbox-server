package edu.kit.provideq.toolbox.meta;

import java.util.Optional;
import java.util.Set;

/**
 * This interface provides access to the {@link ProblemManager problem managers} of all
 * {@link ProblemType problem types}.
 */
public interface ProblemManagerProvider {
  /**
   * Returns the problem managers of all registered problem types.
   */
  Set<ProblemManager<?, ?>> getProblemManagers();

  /**
   * Finds the problem manager for the given problem {@code type}.
   */
  default <InputT, ResultT> Optional<ProblemManager<InputT, ResultT>> findProblemManagerForType(
      ProblemType type
  ) {
    // suppress warning for a cast that we check explicitly by comparing the problem type
    //noinspection unchecked
    return getProblemManagers()
        .stream()
        .filter(manager -> manager.getType().equals(type)) // implies type check
        .map(manager -> (ProblemManager<InputT, ResultT>) manager) // "unchecked" warning
        .findAny();
  }
}
