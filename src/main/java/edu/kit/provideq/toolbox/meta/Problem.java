package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.Solution;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * A problem encapsulates an input for a given problem type.
 * The problem can be solved using a matching ProblemSolver.
 * The {@link Problem} class is essentially a state machine allowing different operations like
 * configuring the input and solver, and starting or stopping the solution process.
 *
 * @param <InputT> the data type of the problem's input.
 * @param <ResultT> the data type of the problem's solution.
 */
public class Problem<InputT, ResultT> {
  private final UUID id;
  private final ProblemType<InputT, ResultT> type;
  private final SubProblems<InputT, ResultT> subProblems;
  private final Set<ProblemObserver<InputT, ResultT>> observers;

  private InputT input;
  private Solution<ResultT> solution;
  private ProblemState state;
  private ProblemSolver<InputT, ResultT> solver;

  /**
   * Creates a new problem of a given {@link ProblemType}.
   *
   * @param type the kind of problem.
   */
  public Problem(ProblemType<InputT, ResultT> type) {
    this.id = UUID.randomUUID();
    this.type = type;

    this.observers = new HashSet<>();

    // Sub-routine management and sub-routine-call handling are outsourced to the SubProblems class
    Consumer<Problem<?, ?>> notifyAdded = addedSubProblem -> this.observers.forEach(
        observer -> observer.onSubProblemAdded(this, addedSubProblem));
    Consumer<Problem<?, ?>> notifyRemoved = removedSubProblem -> this.observers.forEach(
        observer -> observer.onSubProblemRemoved(this, removedSubProblem));
    this.subProblems = new SubProblems<>(notifyAdded, notifyRemoved);
    this.addObserver(subProblems);

    this.setState(ProblemState.NEEDS_CONFIGURATION);
  }

  /**
   * Starts the solution of this problem.
   * Once the problem is solved, the solution can be obtained using {@link #getSolution()}.
   */
  public Mono<Solution<ResultT>> solve() {
    Optional<ProblemSolver<InputT, ResultT>> solverT = getSolver();
    Optional<InputT> inputT = getInput();
    if (solverT.isEmpty() || inputT.isEmpty()) {
      throw new IllegalStateException(
              "The problem %s is not fully configured!".formatted(toString()));
    }

    this.setState(ProblemState.SOLVING);

    long start = System.currentTimeMillis();

    return solverT.get().solve(inputT.get(), subProblems)
        .doOnNext(sol -> {
          long finish = System.currentTimeMillis();
          sol.setExecutionMilliseconds(finish - start);
          this.solution = sol;
          this.setState(ProblemState.SOLVED);
        });
  }

  public UUID getId() {
    return id;
  }

  public ProblemType<InputT, ResultT> getType() {
    return type;
  }

  public Optional<InputT> getInput() {
    return Optional.ofNullable(this.input);
  }

  /**
   * Changes the problem input data.
   */
  public void setInput(InputT newInput) {
    this.input = newInput;

    this.observers.forEach(observer -> observer.onInputChanged(this, newInput));
  }

  public Solution<ResultT> getSolution() {
    return this.solution;
  }

  public Optional<ProblemSolver<InputT, ResultT>> getSolver() {
    return Optional.ofNullable(this.solver);
  }

  /**
   * Changes the problem solver to be used for this problem.
   */
  public void setSolver(ProblemSolver<InputT, ResultT> newSolver) {
    this.solver = newSolver;

    Consumer<ProblemObserver<InputT, ResultT>> update = newSolver == null
        ? observer -> observer.onSolverReset(this)
        : observer -> observer.onSolverChanged(this, newSolver);

    this.observers.forEach(update);
  }

  public ProblemState getState() {
    return this.state;
  }

  @SuppressWarnings("java:S1452")
  public Set<Problem<?, ?>> getSubProblems() {
    return subProblems.getProblems();
  }

  @SuppressWarnings("unchecked")
  public <SubInputT, SubResultT> Set<Problem<SubInputT, SubResultT>> getSubProblems(
          ProblemType<SubInputT, SubResultT> subProblemType) {
    // problem type check guarantees correct type
    return subProblems.getProblems()
            .stream()
            .filter(subProblem -> subProblem.getType().equals(subProblemType))
            .map(subProblem -> (Problem<SubInputT, SubResultT>) subProblem)
            .collect(Collectors.toSet());
  }

  public <SubInputT, SubResultT> Set<Problem<SubInputT, SubResultT>> getSubProblems(
      SubRoutineDefinition<SubInputT, SubResultT> subRoutineDefinition
  ) {
    return subProblems.getProblems(subRoutineDefinition);
  }

  private void setState(ProblemState newState) {
    this.state = newState;

    this.observers.forEach(observer -> observer.onStateChanged(this, newState));
  }

  public void addObserver(ProblemObserver<InputT, ResultT> observer) {
    this.observers.add(observer);
  }

  public void removeObserver(ProblemObserver<InputT, ResultT> observer) {
    this.observers.remove(observer);
  }

  @Override
  public String toString() {
    return "Problem{"
            + "type=" + type
            + ", id=" + id
            + ", state=" + state
            + ", solver=" + solver
            + '}';
  }
}
