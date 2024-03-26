package edu.kit.provideq.toolbox.meta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This helper class can register problems deeply in the corresponding
 * {@link ProblemManager problem managers}.
 */
@Component
public class ProblemRegistration {
  private ProblemManagerProvider managerProvider;

  /**
   * Registers the given problem {@code instance} in the problem manager corresponding to its type.
   * All current and future sub-problems (and sub-sub-problems etc.) will be registered too until
   * they are removed from their parent problem.
   *
   * @param instance the problem instance to register.
   */
  public <InputT, ResultT> void registerProblemInstanceDeeply(Problem<InputT, ResultT> instance) {
    var manager = managerProvider.findProblemManagerForType(instance.getType())
        .orElseThrow(() -> new IllegalStateException(
            "Cannot register problems of unregistered type %s!".formatted(instance.getType())));

    manager.addInstance(instance);
    instance.addObserver(getRegistrationObserver());

    for (var subInstance : instance.getSubProblems()) {
      this.registerProblemInstanceDeeply(subInstance);
    }
  }

  /**
   * This observer makes sure to automatically register new sub-problems once they are added to the
   * observed problem.
   */
  private <InputT, ResultT> ProblemObserver<InputT, ResultT> getRegistrationObserver() {
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
        registerProblemInstanceDeeply(addedSubProblem);
        addedSubProblem.addObserver(getRegistrationObserver());
      }

      @Override
      public <SubInputT, SubResultT> void onSubProblemRemoved(
          Problem<InputT, ResultT> problem,
          Problem<SubInputT, SubResultT> removedSubProblem
      ) {
        // do nothing
      }
    };
  }

  @Autowired
  void setManagerProvider(ProblemManagerProvider managerProvider) {
    this.managerProvider = managerProvider;
  }
}
