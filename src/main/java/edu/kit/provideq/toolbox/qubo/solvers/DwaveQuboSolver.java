package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link QuboConfiguration#QUBO} solver using a Dwaves Quantum Annealer implementation.
 */
@Component
public class DwaveQuboSolver extends QuboSolver {
  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public DwaveQuboSolver(
      @Value("${dwave.directory.qubo}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(D-Wave) Annealing QUBO Solver";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {

    // there is currently no field where a token can be added by the user
    // this field is kept because it was used in Lucas implementation and will be added back later
    // TODO: add user setting that allows passing Tokens
    Optional<String> dwaveToken = Optional.empty();

    // this field is only relevant when a dwaveToken is added
    // (a token is needed to access the d-wave hardware)
    // options are: sim, hybrid, absolv, direct
    String dwaveAnnealingMethod = "sim";

    var solution = new Solution<>(this);

    var processRunner = context.getBean(PythonProcessRunner.class, scriptPath);

    if (dwaveToken.isPresent()) {
      processRunner.withEnvironmentVariable("DWAVE_API_TOKEN", dwaveToken.get());
    }

    var processResult = processRunner
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            dwaveAnnealingMethod,
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH
        )
        .withInputFile(input, "problem.lp")
        .withOutputFile("problem.bin")
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
