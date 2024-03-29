package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SubRoutinePool {
  private final Map<ProblemType, SolveRequest<?>> subRoutineCalls;

  private MetaSolverProvider metaSolverProvider;

  public SubRoutinePool() {
    subRoutineCalls = Collections.emptyMap();
  }

  /**
   * Use this to explicitly define which solver to use for a problem type.
   *
   * @param requestedSubRoutines problem types mapped to a solve requests
   */
  public SubRoutinePool(Map<ProblemType, SolveRequest<?>> requestedSubRoutines) {
    this.subRoutineCalls = Map.copyOf(requestedSubRoutines);
  }

  @Autowired
  public void setProblemControllerProvider(MetaSolverProvider metaSolverProvider) {
    this.metaSolverProvider = metaSolverProvider;
  }

  /**
   * Request a subroutine for a problem type that invokes the solving process that was previously
   * specified.
   * If no subroutine is available, use the default meta solver strategy in the routine.
   *
   * @param problemType problem type to solve
   * @return function to solve a problem of type problemType
   */
  public <ProblemT, SolutionT> Function<ProblemT, Solution<SolutionT>> getSubRoutine(
      ProblemType problemType) {
    return content -> {
      SolveRequest<?> subRoutine = subRoutineCalls.get(problemType);
      if (subRoutine == null) {
        subRoutine = new SolveRequest<>();
      }

      var newSolveRequest = subRoutine.replaceContent(content);

      MetaSolver<ProblemT, SolutionT, ? extends ProblemSolver<ProblemT, SolutionT>> metaSolver =
              (MetaSolver<ProblemT, SolutionT, ? extends ProblemSolver<ProblemT, SolutionT>>)
                      metaSolverProvider.getMetaSolver(problemType);
      return metaSolver.solve(newSolveRequest);
    };
  }
}
