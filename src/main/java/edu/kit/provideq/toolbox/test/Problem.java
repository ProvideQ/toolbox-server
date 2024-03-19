package edu.kit.provideq.toolbox.test;

import edu.kit.provideq.toolbox.ProblemManagerProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.TypedProblemType;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * A problem encapsulates an input for a given problem type.
 * The problem can be solved using a matching ProblemSolver.
 *
 * @param <InputT> the data type of the problem's input.
 * @param <ResultT> the data type of the problem's solution.
 */
public class Problem<InputT, ResultT> {
  private record SubProblemEntry<SubInputT, SubResultT>(
      SubRoutineDefinition<SubInputT, SubResultT> subRoutineDefinition,
      Problem<SubInputT, SubResultT> subProblem) {
  }

  private final UUID id;
  private final TypedProblemType<InputT, ResultT> type;
  private InputT input;
  private Solution<ResultT> solution;
  private ProblemState state;
  private ProblemSolver<InputT, ResultT> solver;
  private final Set<SubProblemEntry<?, ?>> subRoutines;

  /**
   * Creates a new problem of a given {@link ProblemType} with a given {@code input}.
   *
   * @param type the kind of problem.
   */
  public Problem(TypedProblemType<InputT, ResultT> type) {
    this.id = UUID.randomUUID();
    this.type = type;
    this.subRoutines = new HashSet<>();

    this.setState(ProblemState.NEEDS_CONFIGURATION);
  }

  /**
   * Starts the solution of this problem.
   * Once the problem is solved, the solution can be obtained using {@link #getSolution()}.
   */
  public Mono<Solution<ResultT>> solve() {
    if (this.state != ProblemState.READY_TO_SOLVE) {
      throw new IllegalStateException(); // TODO msg
    }

    var weirdlyTypedProblem = new edu.kit.provideq.toolbox.meta.Problem<>(this.input, this.type);
    this.solution = new Solution<>();
    this.setState(ProblemState.SOLVING);
    var subRoutineResolver = getSubRoutineResolver();

    return getSolver()
        .solve(weirdlyTypedProblem, subRoutineResolver)
        .doOnNext(sol -> {
          this.solution = sol;
          this.setState(ProblemState.SOLVED);
        });
  }

  private SubRoutineResolver getSubRoutineResolver() {
    return new SubRoutineResolver() {
      @Override
      public <SubInputT, SubResultT> Mono<Solution<SubResultT>> resolve(
          SubRoutineDefinition<SubInputT, SubResultT> subRoutine,
          SubInputT input
      ) {
        var subProblem = getSubProblem(subRoutine).orElseThrow(() -> new IllegalStateException(
            "Missing sub problem for routine %s!".formatted(subRoutine))
        );
        subProblem.setInput(input);
        return subProblem.solve();
      }
    };
  }


  public <SubInputT, SubResultT> Optional<Problem<SubInputT, SubResultT>> getSubProblem(
      SubRoutineDefinition<SubInputT, SubResultT> subRoutineDefinition
  ) {
    // the generic types of this cast are indeed checked by the preceding filter operation
    //noinspection unchecked
    return this.subRoutines.stream()
        .filter(entry -> entry.subRoutineDefinition == subRoutineDefinition) // type check
        .map(entry -> (SubProblemEntry<SubInputT, SubResultT>) entry)
        .findAny()
        .map(entry -> entry.subProblem);
  }

  /**
   * Sets the solver to use for solving this problem.
   * The solver can only be set once in the {@link ProblemState#NEEDS_CONFIGURATION} state.
   *
   * @param solver the solver to use.
   * @throws IllegalStateException if the solver has already been set.
   */
  public void setSolver(ProblemSolver<InputT, ResultT> solver) {
//    if (this.state != ProblemState.NEEDS_CONFIGURATION || this.solver != null) {
//      throw new IllegalStateException(); // TODO msg
//    } TODO conditions

    boolean solverChanged = this.solver != solver; // TODO see intellij warning
    this.solver = solver;

    if (solverChanged) {
      this.resetSubProblems();
    }

    if (this.isConfigured()) {
      this.setState(ProblemState.READY_TO_SOLVE);
    }
  }

  private void resetSubProblems() {
    // remove previous sub-problems
    ProblemManagerProvider pmp = get();
    for (var sub : this.subRoutines) {
      removeSub(sub);
    }
    this.subRoutines.clear();

    for (var subDef : solver.getSubRoutines()) {
      addSub(subDef);
    }
    // TODO multi subroutines?
  }

  private ProblemManagerProvider get() {
    return null; // TODO how???
  }

  private <SubInputT, SubResultT> void removeSub(SubProblemEntry<SubInputT, SubResultT> sub) {
    ProblemManagerProvider pmp = get();
    var pm = pmp.getProblemManagerForType(sub.subRoutineDefinition.type()).orElseThrow();
    pm.removeProblem(sub.subProblem);
  }

  private <SubInputT, SubResultT> void addSub(
      SubRoutineDefinition<SubInputT, SubResultT> subRoutineDefinition) {
    ProblemManagerProvider pmp = get();

    var problem = new Problem<>(subRoutineDefinition.type());
    pmp.getProblemManagerForType(subRoutineDefinition.type()).orElseThrow()
        .addProblem(problem);
    var entry = new SubProblemEntry<>(subRoutineDefinition, problem);
    this.subRoutines.add(entry);
  }

  /**
   * If a solver has already been set, this method can be used to obtain it.
   *
   * @return the solver chosen to solve this problem. May be {@code null} if the solver is not
   *     configured yet.
   */
  public ProblemSolver<InputT, ResultT> getSolver() {
    return this.solver;
  }

  /**
   * TODO.
   */
  public void setInput(InputT input) {
//    if (this.getState() != ProblemState.NEEDS_CONFIGURATION || this.input != null) {
//      throw new IllegalStateException(); // TODO msg
//    } TODO decide when this is possible and what should happen when this changes

    this.input = input;
    this.solution = null; // TODO: does setInput just reset the thing?
    for (var sub : this.subRoutines) {
      sub.subProblem.setInput(null);
    }

    if (this.isConfigured()) {
      this.setState(ProblemState.READY_TO_SOLVE);
    }
  }

  /**
   * Internal method to check if the configuration is complete.
   */
  private boolean isConfigured() {
    return this.solver != null && this.input != null;
  }

  /**
   * If this problem is already solved, this method can be used to obtain its solution.
   *
   * @return the computed solution for this problem.
   * @throws IllegalStateException if this problem is not already  {@link ProblemState#SOLVED}.
   */
  public Solution<ResultT> getSolution() {
    //if (this.getState() != ProblemState.SOLVED) {
    //  throw new IllegalStateException("Solution unavailable, please check for the SOLVED state.");
    //} TODO

    return this.solution;
  }

  public Set<Problem<?, ?>> getSubRoutines() {
    // TODO in which states can we do this?
    return this.subRoutines.stream().map(entry -> entry.subProblem).collect(Collectors.toSet());
  }

  /**
   * Returns the state of this problem.
   */
  public ProblemState getState() {
    return this.state;
  }

  public TypedProblemType<InputT, ResultT> getType() {
    return this.type;
  }

  /**
   * Only for internal use.
   * TODO: check validity, notify observers
   */
  private void setState(ProblemState newState) {
    this.state = newState;
  }

  /**
   * Returns the id of this problem.
   */
  public UUID getId() {
    return this.id;
  }

  public InputT getInput() {
    return this.input;
  }
}
