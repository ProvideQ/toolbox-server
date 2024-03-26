package edu.kit.provideq.toolbox.meta;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Manages all problem instances of and solvers for a given problem type.
 *
 * @param <InputT> the data type of the problem's input.
 * @param <ResultT> the data type of the problem's solution.
 */
public class ProblemManager<InputT, ResultT> {
  private final ProblemType<InputT, ResultT> type;
  private final Set<ProblemSolver<InputT, ResultT>> solvers;
  private final Set<Problem<InputT, ResultT>> instances;
  private final Set<Problem<InputT, ResultT>> exampleInstances;
  private ProblemManagerProvider provider;
  private final ProblemObserver<InputT, ResultT> registrationObserver = getRegistrationObserver();

  /**
   * Initializes a problem manager for a given problem type.
   * Only one problem manager should be created per {@code type}.
   */
  public ProblemManager(
      ProblemType<InputT, ResultT> type,
      Set<ProblemSolver<InputT, ResultT>> solvers,
      Set<Problem<InputT, ResultT>> exampleInstances
  ) {
    this.type = type;

    this.solvers = new HashSet<>(solvers);
    this.exampleInstances = new HashSet<>(exampleInstances);

    this.instances = new HashSet<>();
  }

  /**
   * Finds a problem instance of this problem manager's {@link #getType() type} with the given
   * {@code id}.
   */
  public Optional<Problem<InputT, ResultT>> findInstanceById(UUID id) {
    return this.instances.stream()
        .filter(instance -> instance.getId().equals(id))
        .findAny();
  }

  /**
   * Finds a problem solver for this problem manager's {@link #getType() type} with the given
   * {@code id}.
   */
  public Optional<ProblemSolver<InputT, ResultT>> findSolverById(String id) {
    return this.solvers.stream()
        .filter(solver -> solver.getId().equals(id))
        .findAny();
  }

  public Set<Problem<InputT, ResultT>> getInstances() {
    return Collections.unmodifiableSet(this.instances);
  }

  /**
   * Registers a problem instance with this problem manager.
   */
  public void addInstance(Problem<InputT, ResultT> instance) {
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
  private <SubInputT, SubResultT> void addSubInstanceToOtherManager(
      Problem<SubInputT, SubResultT> subInstance
  ) {
    var otherProvider = provider.findProblemManagerForType(subInstance.getType())
        .orElseThrow(() -> new IllegalStateException(
            "Cannot register problems of unregistered type %s!".formatted(subInstance.getType()))
        );

    otherProvider.addInstance(subInstance);
  }

  /**
   * Unregisters a problem instance from this problem manager.
   */
  public void removeInstance(Problem<InputT, ResultT> instance) {
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
  private <SubInputT, SubResultT> void removeSubInstanceFromOtherManager(
      Problem<SubInputT, SubResultT> subInstance) {
    var otherProvider = provider.findProblemManagerForType(subInstance.getType())
        .orElseThrow(() -> new IllegalStateException(
            "Cannot unregister problems of unregistered type %s!".formatted(subInstance.getType()))
        );

    otherProvider.removeInstance(subInstance);
  }

  /**
   * Returns a read-only view on the problem solvers registered with this manager.
   */
  public Set<ProblemSolver<InputT, ResultT>> getSolvers() {
    return Collections.unmodifiableSet(this.solvers);
  }

  /**
   * Returns a read-only view on the example problem instances registered with this manager.
   */
  public Set<Problem<InputT, ResultT>> getExampleInstances() {
    return Collections.unmodifiableSet(this.exampleInstances);
  }

  public ProblemType<InputT, ResultT> getType() {
    return type;
  }

  private ProblemObserver<InputT, ResultT> getRegistrationObserver() {
    return new ProblemObserver<>() {
      @Override
      public void onInputChanged(Problem<InputT, ResultT> problem, InputT newInput) {
        // do nothing
      }

      @Override
      public void onSolverChanged(Problem<InputT, ResultT> problem,
                                  ProblemSolver<InputT, ResultT> newSolver) {
        // do nothing
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
        addSubInstanceToOtherManager(addedSubProblem);
      }

      @Override
      public <SubInputT, SubResultT> void onSubProblemRemoved(
          Problem<InputT, ResultT> problem,
          Problem<SubInputT, SubResultT> removedSubProblem
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
