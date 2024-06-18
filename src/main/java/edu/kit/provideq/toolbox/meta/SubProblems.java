package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.Solution;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * This internal class manages sub-problems and sub-routine calls of a {@link Problem}.
 *
 * @apiNote This class is neither intended to be used by other classes than {@link Problem} nor to
 *     be extended or replaced.
 * @implNote {@link SubProblems} is separate to {@link Problem} as it deals with another level of
 *     abstraction.
 *     {@link Problem} is essentially a state machine for the solution process of a problem and
 *     {@link SubProblems} deals with the type-safe wiring between sub-routines and sub-problems.
 *     Additionally, having {@link SubProblems} implement the {@link SubRoutineResolver} interface
 *     avoids interface pollution in the {@link Problem} class.
 */
final class SubProblems<InputT, ResultT>
    implements SubRoutineResolver, ProblemObserver<InputT, ResultT> {

  private static class SubProblemEntry<SubInputT, SubResultT> {
    SubRoutineDefinition<SubInputT, SubResultT> definition;
    List<Problem<SubInputT, SubResultT>> problems;

    SubProblemEntry(SubRoutineDefinition<SubInputT, SubResultT> definition) {
      this.definition = definition;
      this.problems = new ArrayList<>();
    }
  }

  private final Set<SubProblemEntry<?, ?>> entries;
  private final Consumer<Problem<?, ?>> problemAddedObserver;
  private final Consumer<Problem<?, ?>> problemRemovedObserver;
  private final Set<Consumer<Problem<?, ?>>> entryStateChangedObservers;

  /**
   * Initializes a new sub problem manager.
   *
   * @param problemAddedObserver called when a sub-problem instance is added.
   * @param problemRemovedObserver called when a sub-problem instance is removed.
   */
  public SubProblems(
      Consumer<Problem<?, ?>> problemAddedObserver,
      Consumer<Problem<?, ?>> problemRemovedObserver
  ) {
    this.entries = Collections.synchronizedSet(new HashSet<>());
    this.entryStateChangedObservers = Collections.synchronizedSet(new HashSet<>());

    this.problemAddedObserver = problemAddedObserver;
    this.problemRemovedObserver = problemRemovedObserver;
  }

  /**
   * Returns all sub-problems.
   */
  @SuppressWarnings("java:S1452")
  public Set<Problem<?, ?>> getProblems() {
    return entries.stream()
            .map(entry -> entry.problems)
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Returns all sub-problems related to a given sub-routine.
   */
  public <SubResultT, SubInputT> Set<Problem<SubInputT, SubResultT>> getProblems(
      SubRoutineDefinition<SubInputT, SubResultT> subRoutineDefinition) {
    var optionalEntry = findEntry(subRoutineDefinition);

    return optionalEntry
        .map(entry -> (Set<Problem<SubInputT, SubResultT>>) new HashSet<>(entry.problems))
        .orElse(Collections.emptySet());
  }

  @SuppressWarnings("unchecked") // Solution type is always correct
  @Override
  public <SubInputT, SubResultT> Mono<Solution<SubResultT>> runSubRoutine(
      SubRoutineDefinition<SubInputT, SubResultT> subRoutine,
      SubInputT input
  ) {
    var entry = findEntry(subRoutine).orElseThrow(() -> new IllegalArgumentException(
        "The given sub-routine was not registered with the problem solver!"));

    var subProblem = new Problem<>(subRoutine.type());
    subProblem.setInput(input);
    entry.problems.add(subProblem);
    problemAddedObserver.accept(subProblem);
    registerSubProblem(subProblem);

    if (subProblem.getSolver().isPresent()) {
      // In case there is a solver set, we can start the sub problem
      return subProblem.solve();
    }

    // Wait for the solver to be set which will run the solver
    // Once the sub problem is solved, the state changes to SOLVED and at that point we can continue
    return Mono.create(sink -> {
      Consumer<Problem<?, ?>> observer = problem -> {
        if (problem.getId() == subProblem.getId()
                && problem.getState() == ProblemState.SOLVED) {
          sink.success((Solution<SubResultT>) problem.getSolution());
        }
      };

      entryStateChangedObservers.add(observer);

      // Remove the observer once this Mono is consumed and disposed
      sink.onDispose(() -> entryStateChangedObservers.remove(observer));
    });
  }

  @SuppressWarnings("unchecked") // Java cannot infer explicit type check in filter below
  private <SubInputT, SubResultT> Optional<SubProblemEntry<SubInputT, SubResultT>> findEntry(
      SubRoutineDefinition<SubInputT, SubResultT> definition
  ) {
    return entries.stream()
        .filter(entry -> entry.definition.equals(definition)) // explicit type check
        .map(entry -> (SubProblemEntry<SubInputT, SubResultT>) entry)
        .findAny();
  }

  @Override
  public void onInputChanged(Problem<InputT, ResultT> problem, InputT newInput) {
    // do nothing
  }

  @Override
  public void onSolverChanged(
      Problem<InputT, ResultT> problem,
      ProblemSolver<InputT, ResultT> newSolver
  ) {
    for (var entry : entries) {
      for (Problem<?, ?> subProblem : entry.problems) {
        problemRemovedObserver.accept(subProblem);
      }
    }
    entries.clear();

    for (var subRoutine : newSolver.getSubRoutines()) {
      var entry = addEntry(subRoutine);
      for (Problem<?, ?> subProblem : entry.problems) {
        problemAddedObserver.accept(subProblem);
      }
    }
  }

  private <SubInputT, SubResultT> SubProblemEntry<SubInputT, SubResultT> addEntry(
      SubRoutineDefinition<SubInputT, SubResultT> subRoutine
  ) {
    var entry = new SubProblemEntry<>(subRoutine);
    entries.add(entry);
    entry.problems.forEach(this::registerSubProblem);
    return entry;
  }

  private <SubInputT, SubResultT> void registerSubProblem(
          Problem<SubInputT, SubResultT> subProblem
  ) {
    subProblem.addObserver(new ProblemObserver<>() {
      @Override
      public void onInputChanged(Problem<SubInputT, SubResultT> problem, SubInputT newInput) {
        // do nothing
      }

      @Override
      public void onSolverChanged(
              Problem<SubInputT, SubResultT> problem,
              ProblemSolver<SubInputT, SubResultT> newSolver) {
        // do nothing
      }

      @Override
      public void onStateChanged(Problem<SubInputT, SubResultT> problem, ProblemState newState) {
        // Collect in list to avoid ConcurrentModificationException
        var observers = entryStateChangedObservers.stream().toList();
        observers.forEach(observer -> observer.accept(problem));
      }

      @Override
      public <NewSubInputT, NewSubResultT> void onSubProblemAdded(
              Problem<SubInputT, SubResultT> problem,
              Problem<NewSubInputT, NewSubResultT> addedSubProblem) {
        // do nothing
      }

      @Override
      public <NewSubInputT, NewSubResultT> void onSubProblemRemoved(
              Problem<SubInputT, SubResultT> problem,
              Problem<NewSubInputT, NewSubResultT> removedSubProblem) {
        // do nothing
      }
    });
  }

  @Override
  public void onStateChanged(Problem<InputT, ResultT> problem, ProblemState newState) {
    // do nothing
  }

  @Override
  public <SubInputT, SubResultT> void onSubProblemAdded(
      Problem<InputT, ResultT> problem,
      Problem<SubInputT, SubResultT> addedSubProblem
  ) {
    // do nothing, this call should have come from this class anyway
  }

  @Override
  public <SubInputT, SubResultT> void onSubProblemRemoved(
      Problem<InputT, ResultT> problem,
      Problem<SubInputT, SubResultT> removedSubProblem
  ) {
    // do nothing, this call should have come from this class anyway
  }
}
