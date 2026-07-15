package edu.kit.provideq.toolbox.unsplittablemcf.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.unsplittablemcf.UnsplittableMcfConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Classical CPLEX solver for the {@link UnsplittableMcfConfiguration#UNSPLITTABLE_MCF} problem.
 * This solver uses GAMSPy to build the MIP model and solves it using CPLEX.
 */
@Component
public class GamsUnsplittableMcfClassicalSolver extends GamsUnsplittableMcfSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public GamsUnsplittableMcfClassicalSolver(
      @Value("${path.gams.unsplittable-mcf-classical}") String scriptPath,
      @Value("${venv.gams.unsplittable-mcf}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(GAMS) CPLEX Classical MCF Solver";
  }

  @Override
  public String getDescription() {
    return "Solves the Unsplittable Multi Commodity Flow problem using GAMSPy with CPLEX. "
        + "Builds a time-expanded network model and finds optimal flow routes minimizing "
        + "delay and slack penalties.";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    // Run the classical MCF solver via Python process
    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input, "unsplittable-mcf.json")
        .readOutputFile("output.html")
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
