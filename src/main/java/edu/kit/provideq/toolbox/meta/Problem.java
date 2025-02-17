package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.BoundWithInfo;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.api.BoundDto;
import edu.kit.provideq.toolbox.api.ComparisonDto;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
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
  private final ComparisonDto boundWithComparison = new ComparisonDto();
  private ProblemState state;
  private ProblemSolver<InputT, ResultT> solver;
  private List<SolverSetting> solverSettings;

  /**
   * Creates a new problem of a given {@link ProblemType}.
   *
   * @param type the kind of problem.
   */
  public Problem(ProblemType<InputT, ResultT> type) {
    this.id = UUID.randomUUID();
    this.type = type;

    this.observers = new HashSet<>();
    this.solverSettings = List.of();

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

    var properties = new SolvingProperties(getSolverSettings());
    return solverT.get().solve(inputT.get(), subProblems, properties)
        .doOnNext(sol -> {
          long finish = System.currentTimeMillis();
          sol.setExecutionMilliseconds(finish - start);
          this.solution = sol;
          this.setState(ProblemState.SOLVED);
        });
  }

  public void estimateBound() {
    if (this.input == null) {
      throw new IllegalStateException("Cannot estimate value without input!");
    }

    var optionalEstimator = this.type.getEstimator();
    if (optionalEstimator.isEmpty()) {
      throw new IllegalStateException("Cannot estimate value without an estimator!");
    }
    var estimator = optionalEstimator.get();

    long start = System.currentTimeMillis();

    var estimatedBound = estimator.apply(this.input);
    long finish = System.currentTimeMillis();
    var executionTime = finish - start;

    this.boundWithComparison.setBound(new BoundWithInfo(estimatedBound, executionTime));
  }

  public void compareBound() {
    if (this.solution == null) {
      throw new IllegalStateException("Cannot compare bound without solution!");
    }
    if (this.boundWithComparison.getBound() == null) {
      throw new IllegalStateException("Cannot compare bound without bound!");
    }

    var bound = this.boundWithComparison.getBound();
    var solution = this.solution.getSolutionData();

    var pattern = Pattern.compile(this.type.getSolutionPattern());
    var solutionMatcher = pattern.matcher(solution.toString());
    float solutionValue;
    if (solutionMatcher.find()) {
      solutionValue = Float.parseFloat(solutionMatcher.group(1));
    } else {
      throw new IllegalStateException("Solution does not match the expected pattern!");
    }

    var comparison = bound.boundType().compare(bound.bound(), solutionValue);

    this.boundWithComparison.setComparison(comparison);
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

  public Optional<Solution<ResultT>> getSolution() {
    return Optional.ofNullable(this.solution);
  }

  public Optional<ProblemSolver<InputT, ResultT>> getSolver() {
    return Optional.ofNullable(this.solver);
  }

  /**
   * Changes the problem solver to be used for this problem.
   */
  public void setSolver(ProblemSolver<InputT, ResultT> newSolver) {
    this.solver = newSolver;
    this.state = newSolver == null ? ProblemState.NEEDS_CONFIGURATION : ProblemState.READY_TO_SOLVE;

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

  public List<SolverSetting> getSolverSettings() {
    return Collections.unmodifiableList(solverSettings);
  }

  public void setSolverSettings(List<SolverSetting> solverSettings) {
    // Always allow clearing the settings
    if (solverSettings.isEmpty()) {
      this.solverSettings = solverSettings;
      return;
    }

    var optionalSolver = getSolver();
    if (optionalSolver.isEmpty()) {
      throw new IllegalStateException("Cannot set solver settings without a solver!");
    }

    var actualSolver = optionalSolver.get();

    // Check if the solver supports all the settings
    var availableSettings = actualSolver.getSolverSettings()
        .stream()
        .map(SolverSetting::getName)
        .collect(Collectors.toSet());

    for (SolverSetting setting : solverSettings) {
      if (!availableSettings.contains(setting.getName())) {
        throw new IllegalArgumentException("The solver %s does not support the setting %s!"
            .formatted(actualSolver.getName(), setting.getName()));
      }
    }

    this.solverSettings = solverSettings;
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

  public Optional<BoundDto> getBound() {
    return Optional.ofNullable(boundWithComparison.getBound());
  }

  public Optional<ComparisonDto> getBoundWithComparison() {
    return Optional.of(boundWithComparison);
  }
}
