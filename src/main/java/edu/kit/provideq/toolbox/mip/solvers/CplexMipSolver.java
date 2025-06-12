package edu.kit.provideq.toolbox.mip.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.mip.MipConfiguration;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link MipConfiguration#MIP} solver using IBM cplex.
 */
@Component
public class CplexMipSolver extends MipSolver {
  private final String scriptPath;
  private final String venv;

  private final ApplicationContext context;

  @Autowired
  public CplexMipSolver(
      @Value("${path.cplex.mip}") String scriptPath,
      @Value("${venv.cplex.mip}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Cplex";
  }

  @Override
  public String getDescription() {
    return "This solver uses IBM Cplex Solver to solve MIP problems. "
            + "It is called via Python wrapper without any additional parameters";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input)
        .readOutputFile("output.txt")
        .run(getProblemType(), solution.getId());

    // Return if process failed
    return Mono.just(processResult.applyTo(solution));
  }
}
