package edu.kit.provideq.toolbox.mip.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.GamsProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import java.io.File;
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
        .writeInputFile(input, "problem.lp")
        .readOutputFile("problem.lp")
        .run(getProblemType(), solution.getId());

    String relativePath = "jobs/mip/" + solution.getId() + "/problem.gms";

    ProcessResult<String> processResult = context
        .getBean(GamsProcessRunner.class, relativePath)
        .withArguments(
            "--IDIR=" + new File(dependencyScriptPath).getAbsolutePath(),
            "--SOLOUTPUT=" + ProcessRunner.OUTPUT_FILE_PATH
        )
        .readOutputFile("output.txt")
        .run(getProblemType(), solution.getId());
    return Mono.just(processResult.applyTo(solution));
  }
}
