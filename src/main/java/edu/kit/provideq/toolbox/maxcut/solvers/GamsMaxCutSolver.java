package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.maxcut.MaxCutConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.GamsProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessRunner;
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
  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public GamsMaxCutSolver(
      @Value("${gams.script.max-cut}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "GAMS MaxCut";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    // Run MaxCut with GAMS via console
    var processResult = context
        .getBean(GamsProcessRunner.class, scriptPath)
        .withArguments(
            "--INPUT=" + ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .withInputFile(input)
        .withOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
