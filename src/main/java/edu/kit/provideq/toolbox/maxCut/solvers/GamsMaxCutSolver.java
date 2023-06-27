package edu.kit.provideq.toolbox.maxCut.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class GamsMaxCutSolver extends MaxCutSolver {
  private final String maxCutPath;
  private final ApplicationContext context;

  @Autowired
  public GamsMaxCutSolver(
      @Value("${gams.directory.max-cut}") String maxCutPath,
      ApplicationContext context) {
    this.maxCutPath = maxCutPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "GAMS MaxCut";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    //TODO: assess problemData
    return problem.type() == ProblemType.MAX_CUT;
  }

  @Override
  public float getSuitability(Problem<String> problem) {
    //TODO: implement algorithm for suitability calculation
    return 1;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool) {
    // Run MaxCut with GAMS via console
    var processResult = context
        .getBean(
            GamsProcessRunner.class,
            maxCutPath,
            "maxcut.gms")
        .run(problem.type(), solution.getId(), problem.problemData());

    if (processResult.success()) {
      solution.setSolutionData(processResult.output());
      solution.complete();
    } else {
      solution.setDebugData(processResult.output());
      solution.abort();
    }
  }
}
