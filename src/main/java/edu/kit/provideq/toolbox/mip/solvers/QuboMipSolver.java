package edu.kit.provideq.toolbox.mip.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.GamsProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class QuboMipSolver extends MipSolver {
  private final String translationScriptPath;
  private final ApplicationContext context;

  @Autowired
  public QuboMipSolver(
      @Value("${ortools.script.ormip}") String translationScriptPath,
      ApplicationContext context) {
    this.translationScriptPath = translationScriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "TODO";
  }

  @Override
  public String getDescription() {
    return "TODO";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    // Run GAMS via console
    ProcessResult<String> processResult = context
        .getBean(GamsProcessRunner.class, translationScriptPath)
        .withArguments(
            "--INPUT=" + ProcessRunner.INPUT_FILE_PATH,
            "--SOLOUTPUT=" + ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input, "problem.lp")
        .readOutputFile()
        .run(getProblemType(), solution.getId());
    return Mono.just(processResult.applyTo(solution));
  }
}
