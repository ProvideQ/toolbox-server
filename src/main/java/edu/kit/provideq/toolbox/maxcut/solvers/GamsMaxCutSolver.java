package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.gml.Gml;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
  public boolean canSolve(Problem<String> problem) {
    //TODO: assess problemData
    return problem.type() == ProblemType.MAX_CUT;
  }

  @Override
  public Mono<Solution<String>> solve(Problem<String> problem,
                                      Solution<String> solution,
                                      SubRoutinePool subRoutinePool) {
    // Run MaxCut with GAMS via console
    var processResult = context
        .getBean(
            GamsProcessRunner.class,
            maxCutPath,
            "maxcut.gms")
        .run(problem.type(), solution.getId(), problem.problemData());

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
