package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link QuboConfiguration#QUBO} solver using a GAMS CPLEX implementation.
 */
@Component
public class GamsQuboSolver extends QuboSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public GamsQuboSolver(
      @Value("${path.gams.qubo}") String scriptPath,
      @Value("${venv.gams.qubo}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(GAMS) CPLEX Solver for QUBOs";
  }

  @Override
  public String getDescription() {
    return "A solver for QUBOs using a GAMS CPLEX solver.";
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
        .writeInputFile(input, "problem.lp")
        .readOutputFile("solution.json")
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
