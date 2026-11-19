package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.families.FormatFamily;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Manages all problem instances of and solvers for a given problem type.
 *
 * @param <InT> the data type of the problem's input.
 * @param <OutT> the data type of the problem's solution.
 */
public class ProblemManager<InT extends FormatFamily, OutT extends FormatFamily> {
  private final ProblemType<InT, OutT> type;
  private final Set<ProblemSolver<InT, OutT>> solvers;
  private final Set<Problem<InT, OutT>> instances;
  private final Set<Problem<InT, OutT>> exampleInstances;
  private ProblemManagerProvider provider;
  private final ProblemObserver<InT, OutT> registrationObserver = getRegistrationObserver();

  /**
   * Initializes a problem manager for a given problem type without any solvers.
   * Solvers are registered afterwards using {@link #registerSolver(ProblemSolver)}.
   * Only one problem manager should be created per {@code type}.
   */
  public ProblemManager(
      ProblemType<InT, OutT> type,
      Set<Problem<InT, OutT>> exampleInstances
  ) {
    this.type = type;

    this.solvers = new HashSet<>();
    this.exampleInstances = new HashSet<>(exampleInstances);

    this.instances = new HashSet<>();
  }

  /**
   * Registers a problem solver with this problem manager. The solver's problem type must match
   * this manager's {@link #getType() type}. This is also the entry point for external code to
   * add custom solvers to an existing problem manager.
   *
   * @param solver the solver to register
   * @throws IllegalArgumentException if the solver's problem type does not match this
   *         manager's type
   */
  public void registerSolver(ProblemSolver<InT, OutT> solver) {
    if (!solver.getProblemType().equals(this.type)) {
      throw new IllegalArgumentException(
          "Solver %s cannot be registered with problem manager of type %s!"
              .formatted(solver.getId(), this.type));
    }
    this.solvers.add(solver);
  }

  /**
   * Registers multiple problem solvers with this problem manager,
   * see {@link #registerSolver(ProblemSolver)}.
   *
   * @param solvers the solvers to register
   */
  @SafeVarargs
  public final void registerSolvers(final ProblemSolver<InT, OutT>... solvers) {
    for (var solver : solvers) {
      registerSolver(solver);
    }
  }

  /**
   * Finds a problem instance of this problem manager's {@link #getType() type} with the given
   * {@code id}.
   */
  public Optional<Problem<InT, OutT>> findInstanceById(UUID id) {
    return this.instances.stream()
        .filter(instance -> instance.getId().equals(id))
        .findAny();
  }

  /**
   * Finds a problem solver for this problem manager's {@link #getType() type} with the given
   * {@code id}.
   */
  public Optional<ProblemSolver<InT, OutT>> findSolverById(String id) {
    return this.solvers.stream()
        .filter(solver -> solver.getId().equals(id))
        .findAny();
  }

  public Set<Problem<InT, OutT>> getInstances() {
    return Collections.unmodifiableSet(this.instances);
  }

  /**
   * Registers a problem instance with this problem manager.
   */
  public void addInstance(Problem<InT, OutT> instance) {
    this.instances.add(instance);
    instance.addObserver(this.registrationObserver);

    for (var subInstance : instance.getSubProblems()) {
      addSubInstanceToOtherManager(subInstance);
    }
  }

  /**
   * Helper function for {@link #addInstance(Problem)} for registering sub-instances with
   * type-safety.
   */
  private <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      void addSubInstanceToOtherManager(Problem<SubInT, SubOutT> subInstance) {
    var otherProvider = provider.findProblemManagerForType(subInstance.getType())
        .orElseThrow(() -> new IllegalStateException(
            "Cannot register problems of unregistered type %s!".formatted(subInstance.getType()))
        );

    otherProvider.addInstance(subInstance);
  }

  /**
   * Unregisters a problem instance from this problem manager.
   */
  public void removeInstance(Problem<InT, OutT> instance) {
    this.instances.remove(instance);
    instance.removeObserver(this.registrationObserver);

    for (var subInstance : instance.getSubProblems()) {
      removeSubInstanceFromOtherManager(subInstance);
    }
  }

  /**
   * Helper function for {@link #removeInstance(Problem)} for unregistering sub-instances with
   * type-safety.
   */
  private <SubInT extends FormatFamily, SubOutT extends FormatFamily>
      void removeSubInstanceFromOtherManager(Problem<SubInT, SubOutT> subInstance) {
    var otherProvider = provider.findProblemManagerForType(subInstance.getType())
        .orElseThrow(() -> new IllegalStateException(
            "Cannot unregister problems of unregistered type %s!".formatted(subInstance.getType()))
        );

    otherProvider.removeInstance(subInstance);
  }

  /**
   * Returns a read-only view on the problem solvers registered with this manager.
   */
  public Set<ProblemSolver<InT, OutT>> getSolvers() {
    return Collections.unmodifiableSet(this.solvers);
  }

  /**
   * Returns a read-only view on the example problem instances registered with this manager.
   */
  public Set<Problem<InT, OutT>> getExampleInstances() {
    return Collections.unmodifiableSet(this.exampleInstances);
  }

  public ProblemType<InT, OutT> getType() {
    return type;
  }

  private ProblemObserver<InT, OutT> getRegistrationObserver() {
    return new ProblemObserver<>() {
      @Override
      public void onInputChanged(Problem<InT, OutT> problem, FormattedValue<?> newInput) {
        // do nothing
      }

      @Override
      public void onSolverChanged(Problem<InT, OutT> problem,
                                  ProblemSolver<InT, OutT> newSolver) {
        // do nothing
      }

      @Override
      public void onSolverReset(Problem<InT, OutT> problem) {
        // do nothing
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
        addSubInstanceToOtherManager(addedSubProblem);
      }

      @Override
      public <SubInT extends FormatFamily, SubOutT extends FormatFamily>
          void onSubProblemRemoved(
              Problem<InT, OutT> problem,
              Problem<SubInT, SubOutT> removedSubProblem
      ) {
        removeSubInstanceFromOtherManager(removedSubProblem);
      }
    };
  }

  @Autowired
  void setProvider(ProblemManagerProvider provider) {
    this.provider = provider;
  }
}