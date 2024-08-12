package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
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
  private final String quboScriptPath;
  private final ApplicationContext context;

  @Autowired
  public DwaveQuboSolver(
      @Value("${dwave.directory.qubo}") String quboScriptPath,
      ApplicationContext context) {
    this.quboScriptPath = quboScriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(D-Wave) Annealing QUBO Solver";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver
  ) {

    // there is currently no field where a token can be added by the user
    // this field is kept because it was used in Lucas implementation and will be added back later
    Optional<String> dwaveToken = Optional.empty();

    // this field is only relevant when a dwaveToken is added
    // (a token is needed to access the d-wave hardware)
    // options are: sim, hybrid, absolv, direct
    String dwaveAnnealingMethod = "sim";

    var solution = new Solution<String>();

    var processRunner = context.getBean(
            PythonProcessRunner.class,
            quboScriptPath,
            "main.py",
            new String[] {"%1$s", dwaveAnnealingMethod, "--output-file", "%2$s"}
        )
        .problemFileName("problem.lp")
        .solutionFileName("problem.bin");

    if (dwaveToken.isPresent()) {
      processRunner.addEnvironmentVariable("DWAVE_API_TOKEN", dwaveToken.get());
    }

    var processResult = processRunner
        .run(getProblemType(), solution.getId(), input);

    return Mono.just(processResult.applyTo(solution));
  }
}
