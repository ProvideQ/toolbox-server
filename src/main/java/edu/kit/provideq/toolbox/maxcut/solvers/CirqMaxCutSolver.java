package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.maxcut.MaxCutConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link MaxCutConfiguration#MAX_CUT} solver using a Cirq QAOA implementation.
 */
@Component
public class CirqMaxCutSolver extends MaxCutSolver {
  private final ApplicationContext context;
  private final String scriptPath;
  private final String venv;

  @Autowired
  public CirqMaxCutSolver(
      @Value("${path.cirq.max-cut}") String scriptPath,
      @Value("${venv.cirq.max-cut}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Cirq MaxCut";
  }

  @Override
  public String getDescription() {
    return "Solves the MaxCut problem using a Cirq QAOA implementation.";
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
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
