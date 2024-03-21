package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SubRoutinePool {
  private ProblemManagerProvider problemManagerProvider;

  /**
   * Request a subroutine for a problem type that invokes the solving process that was previously
   * specified.
   * If no subroutine is available, use the default meta solver strategy in the routine.
   *
   * @param problemType problem type to solve
   * @return function to solve a problem of type problemType
   */
  public <ProblemT, ResultT> Function<ProblemT, Solution<ResultT>> getSubRoutine(
      ProblemType<ProblemT, ResultT> problemType) {
    return content -> {
      var manager = problemManagerProvider.findProblemManagerForType(problemType).orElseThrow();
      var solver = manager.getSolvers().stream().findAny().orElseThrow();

      var solution = new Solution<ResultT>();
      solver.solve(content, solution, this);
      return solution;
    };
  }

  @Autowired
  public void setProblemManagerProvider(ProblemManagerProvider problemManagerProvider) {
    this.problemManagerProvider = problemManagerProvider;
  }
}
