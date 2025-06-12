package edu.kit.provideq.toolbox.mip.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.mip.MipConfiguration;
import edu.kit.provideq.toolbox.process.GamsProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link MipConfiguration#MIP} solver using QUBO Reformulation implementing gams.
 */
@Component
public class QuboMipSolver extends MipSolver {
  private final String mipToGamsReformulatorPath;
  private final String solverScriptPath;
  private final ApplicationContext context;
  private static final int PENALTY = 100;

  @Autowired
  public QuboMipSolver(
      @Value("${path.gams.mip-to-gams-reformulation}") String mipToGamsReformulatorPath,
      @Value("${path.gams.gams-to-qubo-reformulation}") String solverScriptPath,
      ApplicationContext context) {
    this.mipToGamsReformulatorPath = mipToGamsReformulatorPath;
    this.solverScriptPath = solverScriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Gams Solver for MIP";
  }

  @Override
  public String getDescription() {
    return "This solver uses LP / MIP reformulation to QUBO which "
        + "is then subsequently solved by gams";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    // Run GAMS via console
    context
        .getBean(GamsProcessRunner.class, mipToGamsReformulatorPath)
        .withArguments(
            "--INPUT=" + ProcessRunner.INPUT_FILE_PATH,
            "--PENALTY=" + PENALTY
        )
        .writeInputFile(input, "problem.lp")
        .readOutputFile("problem.lp")
        .run(getProblemType(), solution.getId());

    String relativePath = "jobs/mip/" + solution.getId() + "/problem.gms";

    ProcessResult<String> processResult = context
        .getBean(GamsProcessRunner.class, relativePath)
        .withArguments(
            "--IDIR=" + new File(solverScriptPath).getAbsolutePath(),
            "--SOLOUTPUT=" + ProcessRunner.OUTPUT_FILE_PATH
        )
        .readOutputFile("output.txt")
        .run(getProblemType(), solution.getId());
    return Mono.just(processResult.applyTo(solution));
  }
}
