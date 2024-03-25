package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.maxcut.MaxCutConfiguration;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link MaxCutConfiguration#MAX_CUT} solver using a GAMS implementation.
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
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver
  ) {
    var solution = new Solution<String>();

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
      return Mono.just(solution);
    }

    solution.setSolutionData(processResult.output());
    solution.complete();
    return Mono.just(solution);
  }
}
