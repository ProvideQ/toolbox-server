package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
final class SubProblems<InT extends FormatFamily, OutT extends FormatFamily>
    implements SubRoutineResolver, ProblemObserver<InT, OutT> {

  private static class SubProblemEntry<InT extends FormatFamily, OutT extends FormatFamily> {
    final SubRoutineDefinition<InT, OutT> definition;
    final List<Problem<InT, OutT>> problems;

    SubProblemEntry(SubRoutineDefinition<InT, OutT> definition) {
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
   * Returns all sub-problems related to a given sub-routine, in the order they were created in.
   */
  public <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      Set<Problem<SubInT, SubOutT>> getProblems(
          SubRoutineDefinition<SubInT, SubOutT> subRoutineDefinition) {
    var optionalEntry = findEntry(subRoutineDefinition);

    return optionalEntry
        .map(entry -> (Set<Problem<SubInT, SubOutT>>) new LinkedHashSet<>(entry.problems))
        .orElse(Collections.emptySet());
  }

  @Override
  public <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      Mono<Solution<SubOutT>> runSubRoutine(
          SubRoutineDefinition<SubInT, SubOutT> subRoutine,
          FormattedValue<?> input,
          ConversionService conversionService) {
    var entry = findEntry(subRoutine).orElseThrow(() -> new IllegalArgumentException(
        "The given sub-routine was not registered with the problem solver!"));

    var subProblem = new Problem<>(subRoutine.type());
    subProblem.setInput(input);
    entry.problems.add(subProblem);
    problemAddedObserver.accept(subProblem);
    registerSubProblem(subProblem);

    if (subProblem.getSolver().isPresent()) {
      // In case there is a solver set, we can start the sub problem
      return subProblem.solve(conversionService);
    }

    // Wait for the solver to be set which will run the solver
    // Once the sub problem is solved, the state changes to SOLVED and at that point we can continue
    return Mono.create(sink -> {
      Consumer<Problem<?, ?>> observer = problem -> {
        if (problem.getId() == subProblem.getId()
                && problem.getState() == ProblemState.SOLVED) {
          @SuppressWarnings("unchecked") // Solution type is always correct
          var solution = (Solution<SubOutT>) problem.getSolution().orElseThrow();
          sink.success(solution);
        }
      };

      entryStateChangedObservers.add(observer);

      // Remove the observer once this Mono is consumed and disposed
      sink.onDispose(() -> entryStateChangedObservers.remove(observer));
    });
  }

  @SuppressWarnings("unchecked") // Java cannot infer explicit type check in filter below
  private <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      Optional<SubProblemEntry<SubInT, SubOutT>> findEntry(
          SubRoutineDefinition<SubInT, SubOutT> definition) {
    return entries.stream()
        .filter(entry -> entry.definition.equals(definition)) // explicit type check
        .map(entry -> (SubProblemEntry<SubInT, SubOutT>) entry)
        .findAny();
  }

  @Override
  public void onInputChanged(Problem<InT, OutT> problem, FormattedValue<?> newInput) {
    // do nothing
  }

  @Override
  public void onSolverChanged(
      Problem<InT, OutT> problem,
      ProblemSolver<InT, OutT> newSolver
  ) {
    onSolverReset(problem);

    for (var subRoutine : newSolver.getSubRoutines()) {
      var entry = addEntry(subRoutine);
      for (Problem<?, ?> subProblem : entry.problems) {
        problemAddedObserver.accept(subProblem);
      }
    }
  }

  @Override
  public void onSolverReset(Problem<InT, OutT> problem) {
    for (var entry : entries) {
      for (Problem<?, ?> subProblem : entry.problems) {
        problemRemovedObserver.accept(subProblem);
      }
    }
    entries.clear();
  }

  private <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      SubProblemEntry<SubInT, SubOutT> addEntry(SubRoutineDefinition<SubInT, SubOutT> subRoutine) {
    var entry = new SubProblemEntry<>(subRoutine);
    entries.add(entry);
    entry.problems.forEach(this::registerSubProblem);
    return entry;
  }

  private <SubInT extends FormatFamily, SubOutT extends FormatFamily> void registerSubProblem(
          Problem<SubInT, SubOutT> subProblem
  ) {
    subProblem.addObserver(new ProblemObserver<>() {
      @Override
      public void onInputChanged(Problem<SubInT, SubOutT> problem, FormattedValue<?> newInput) {
        // do nothing
      }

      @Override
      public void onSolverChanged(
              Problem<SubInT, SubOutT> problem,
              ProblemSolver<SubInT, SubOutT> newSolver) {
        // do nothing
      }

      @Override
      public void onSolverReset(Problem<SubInT, SubOutT> problem) {
        // do nothing
      }

      @Override
      public void onStateChanged(Problem<SubInT, SubOutT> problem, ProblemState newState) {
        // Collect in list to avoid ConcurrentModificationException
        var observers = entryStateChangedObservers.stream().toList();
        observers.forEach(observer -> observer.accept(problem));
      }

      @Override
      public <NewSubInT extends FormatFamily, NewSubOutT extends FormatFamily>
          void onSubProblemAdded(
              Problem<SubInT, SubOutT> problem,
              Problem<NewSubInT, NewSubOutT> addedSubProblem) {
        // do nothing
      }

      @Override
      public <NewSubInT extends FormatFamily, NewSubOutT extends FormatFamily>
          void onSubProblemRemoved(
              Problem<SubInT, SubOutT> problem,
              Problem<NewSubInT, NewSubOutT> removedSubProblem) {
        // do nothing
      }
    });
  }

  @Override
  public void onStateChanged(Problem<InT, OutT> problem, ProblemState newState) {
    // do nothing
  }

  @Override
  public <SubInT extends FormatFamily, SubOutT extends FormatFamily> void onSubProblemAdded(
      Problem<InT, OutT> problem,
      Problem<SubInT, SubOutT> addedSubProblem
  ) {
    // do nothing, this call should have come from this class anyway
  }

  @Override
  public <SubInT extends FormatFamily, SubOutT extends FormatFamily> void onSubProblemRemoved(
      Problem<InT, OutT> problem,
      Problem<SubInT, SubOutT> removedSubProblem
  ) {
    // do nothing, this call should have come from this class anyway
  }
}
