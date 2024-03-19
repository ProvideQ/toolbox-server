package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#MAX_CUT} solver using a GAMS implementation.
 */
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
  public void solve(String input, Solution<String> solution,
                    SubRoutinePool subRoutinePool) {
    // Run MaxCut with GAMS via console
    var processResult = context
        .getBean(
            GamsProcessRunner.class,
            maxCutPath,
            "maxcut.gms")
        .run(getProblemType(), solution.getId(), input);

    // Return if process failed
    if (!processResult.success()) {
      solution.setDebugData("GAMS process failed: " + processResult.output());
      solution.fail();
      return;
    }

    solution.setSolutionData(processResult.output());
    solution.complete();
  }
}
