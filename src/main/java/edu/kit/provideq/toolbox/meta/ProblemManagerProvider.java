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
  @SuppressWarnings("java:S1452") // ProblemManager is a raw type
  Set<ProblemManager<?, ?>> getProblemManagers();

  /**
   * Finds the problem manager for the given problem {@code type}.
   */
  @SuppressWarnings("unchecked") // compiler does not recognize implicit type check, see below
  default <InputT, ResultT> Optional<ProblemManager<InputT, ResultT>> findProblemManagerForType(
      ProblemType<InputT, ResultT> type
  ) {
    return getProblemManagers()
        .stream()
        .filter(manager -> manager.getType().equals(type)) // implies type check
        .map(manager -> (ProblemManager<InputT, ResultT>) manager) // "unchecked" warning
        .findAny();
  }
}
