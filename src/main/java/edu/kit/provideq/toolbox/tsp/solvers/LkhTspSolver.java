package edu.kit.provideq.toolbox.tsp.solvers;

import edu.kit.provideq.toolbox.Solution;
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
 * Classical Solver for the TSP Problem that uses the LKH-3 heuristics.
 */
@Component
public class LkhTspSolver extends TspSolver {
  private final String scriptPath;
  private final String binaryPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public LkhTspSolver(
      @Value("${path.custom.lkh}") String scriptPath,
      @Value("${path.custom.lkh.binary}") String binaryPath,
      @Value("${venv.custom.lkh}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.binaryPath = binaryPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "LKH-3 TSP Solver";
  }

  @Override
  public String getDescription() {
    return "Solver for the TSP Problem using the LKH-3 heuristics.";
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
            "--lkh-instance", binaryPath,
            ProcessRunner.INPUT_FILE_PATH,
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(adaptInput(input), "problem.vrp")
        .readOutputFile("problem.sol")
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }

  /**
   * LKH-3 solver has an issue when the "EOF" tag is used in a TSP file.
   * This method removes this substring.
   *
   * @param originalInput original input of the TSP problem
   * @return adapted input with "EOF"
   */
  private String adaptInput(String originalInput) {
    String inputAsVrp = originalInput;
    if (inputAsVrp.endsWith("EOF")) {
      inputAsVrp = inputAsVrp.replaceAll("EOF$", "");
    }
    return inputAsVrp;
  }
}
