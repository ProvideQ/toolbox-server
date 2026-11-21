package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.BoundWithInfo;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.api.BoundComparisonDto;
import edu.kit.provideq.toolbox.api.BoundDto;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.exception.SolverException;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * A problem encapsulates an input for a given problem type.
 * The problem can be solved using a matching ProblemSolver.
 * The {@link Problem} class is essentially a state machine allowing different operations like
 * configuring the input and solver, and starting or stopping the solution process.
 *
 * @param <InT> the input format family of the problem.
 * @param <OutT> the output format family of the problem.
 */
public class Problem<InT extends FormatFamily, OutT extends FormatFamily> {
  private static final Logger LOGGER = Logger.getLogger(Problem.class.getName());

  private final UUID id;
  private final ProblemType<InT, OutT> type;
  private final SubProblems<InT, OutT> subProblems;
  private final Set<ProblemObserver<InT, OutT>> observers;

  private FormattedValue<?> input;
  private Solution<OutT> solution;
  private BoundComparisonDto boundWithComparison;
  private ProblemState state;
  private ProblemSolver<InT, OutT> solver;
  private List<SolverSetting> solverSettings;

  /**
   * Creates a new problem of a given {@link ProblemType} without conversion support.
   * Input values of such problems cannot derive other representations.
   *
   * @param type the kind of problem.
   */
  public Problem(ProblemType<InT, OutT> type) {
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
  public Mono<Solution<OutT>> solve(
      ConversionService conversionService) {
    Optional<ProblemSolver<InT, OutT>> solverT = getSolver();
    Optional<FormattedValue<?>> inputT = getInput();
    if (solverT.isEmpty() || inputT.isEmpty()) {
      throw new IllegalStateException(
              "The problem %s is not fully configured!".formatted(toString()));
    }

    this.setState(ProblemState.SOLVING);

    long start = System.currentTimeMillis();

    var properties = new SolvingProperties(getSolverSettings());
    // Solver exceptions are converted into completed solutions here, so that solvers can
    // simply throw (e.g. from convert) instead of building and returning failure
    // solutions themselves. All solver invocations, including sub-routines, route through
    // this method and are therefore covered by this handling.
    return Mono.defer(() -> solverT.get()
            .solve(inputT.get(), subProblems, properties, conversionService))
        // format conversion failed -> aborted (INVALID)
        .onErrorResume(ConversionException.class, e ->
            Mono.just(Solution.aborted(solverT.get(), e.getMessage())))
        // solver failed -> failed (ERROR)
        .onErrorResume(SolverException.class, e ->
            Mono.just(Solution.failed(solverT.get(), e.getMessage())))
        // fallback: unexpected error -> failed (ERROR)
        .onErrorResume(e -> {
          LOGGER.severe("Unexpected error in solver %s: %s"
              .formatted(solverT.get().getName(), e));
          return Mono.just(Solution.failed(solverT.get(), "Unexpected error: " + e));
        })
        .doOnNext(sol -> {
          long finish = System.currentTimeMillis();
          sol.setExecutionMilliseconds(finish - start);
          this.solution = sol;
          this.setState(ProblemState.SOLVED);
        });
  }

  /**
   * Estimates a bound for the problem's solution. Uses the estimator provided by the problem type.
   * Also sets the execution time of the estimation in the boundWithComparison object.
   */
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

    this.boundWithComparison =
        new BoundComparisonDto(new BoundWithInfo(estimatedBound, executionTime));
  }

  /**
   * Compares the current solution with the bound according to the bound type.
   */
  public void compareBound() {
    if (this.solution == null) {
      throw new IllegalStateException("Cannot compare bound without solution!");
    }
    if (this.boundWithComparison == null || !this.boundWithComparison.hasBound()) {
      throw new IllegalStateException("Cannot compare bound without bound!");
    }

    var bound = this.boundWithComparison.getBound();
    var solutionData = this.solution.getSolutionData();

    float solutionValue = getSolutionValue(solutionData);

    var comparison = bound.boundType().compare(bound.bound(), solutionValue);

    this.boundWithComparison.setComparison(comparison);
  }

  public UUID getId() {
    return id;
  }

  public ProblemType<InT, OutT> getType() {
    return type;
  }

  public Optional<FormattedValue<?>> getInput() {
    return Optional.ofNullable(this.input);
  }

  /**
   * Changes the problem input data.
   */
  public void setInput(FormattedValue<?> newInput) {
    this.input = newInput;

    this.observers.forEach(observer -> observer.onInputChanged(this, newInput));
  }

  public Optional<Solution<OutT>> getSolution() {
    return Optional.ofNullable(this.solution);
  }

  public Optional<ProblemSolver<InT, OutT>> getSolver() {
    return Optional.ofNullable(this.solver);
  }

  /**
   * Changes the problem solver to be used for this problem.
   */
  public void setSolver(ProblemSolver<InT, OutT> newSolver) {
    this.solver = newSolver;
    this.state = newSolver == null ? ProblemState.NEEDS_CONFIGURATION : ProblemState.READY_TO_SOLVE;

    Consumer<ProblemObserver<InT, OutT>> update = newSolver == null
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
  public <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      Set<Problem<SubInT, SubOutT>> getSubProblems(ProblemType<SubInT, SubOutT> subProblemType) {
    // problem type check guarantees correct type
    return subProblems.getProblems()
            .stream()
            .filter(subProblem -> subProblem.getType().equals(subProblemType))
            .map(subProblem -> (Problem<SubInT, SubOutT>) subProblem)
            .collect(Collectors.toSet());
  }

  public <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      Set<Problem<SubInT, SubOutT>> getSubProblems(
          SubRoutineDefinition<SubInT, SubOutT> subRoutineDefinition
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

  public void addObserver(ProblemObserver<InT, OutT> observer) {
    this.observers.add(observer);
  }

  public void removeObserver(ProblemObserver<InT, OutT> observer) {
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
    if (boundWithComparison == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(boundWithComparison.getBound());
  }

  public Optional<BoundComparisonDto> getBoundWithComparison() {
    return Optional.ofNullable(boundWithComparison);
  }

  private float getSolutionValue(FormattedValue<?> solutionData) {
    var pattern = Pattern.compile(this.type.getSolutionPattern());
    String solutionText = solutionData.toString();
    var solutionMatcher = pattern.matcher(solutionText);
    float solutionValue;
    if (solutionMatcher.find()) {
      solutionValue = Float.parseFloat(solutionMatcher.group(1));
    } else {
      throw new IllegalStateException("Solution does not match the expected pattern!");
    }
    return solutionValue;
  }
}