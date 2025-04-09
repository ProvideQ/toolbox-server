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
  private final String scriptPath;
  private final String dependencyScriptPath;
  private final ApplicationContext context;
  public static final int PENALTY = 100;

  @Autowired
  public QuboMipSolver(
      @Value("${gams.script.mip}") String scriptPath,
      @Value("${gams.script.qubo_solve}") String dependencyScriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.dependencyScriptPath = dependencyScriptPath;
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
    ProcessResult<String> fileGeneratorResult = context
        .getBean(GamsProcessRunner.class, scriptPath)
        .withArguments(
            "--INPUT=" + ProcessRunner.INPUT_FILE_PATH,
            "--PENALTY=" + String.valueOf(PENALTY)
        )
        .writeInputFile(input)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    String modifiedInputFilePath = ProcessRunner.INPUT_FILE_PATH.replaceFirst("\\.[^.]*$", ".gms");

    ProcessResult<String> processResult = context
        .getBean(GamsProcessRunner.class, modifiedInputFilePath)
        .withArguments(
            "--IDIR=" + dependencyScriptPath,
            "--SOLOUTPUT=" + ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input)
        .readOutputFile()
        .run(getProblemType(), solution.getId());
    return Mono.just(processResult.applyTo(solution));
  }
}
