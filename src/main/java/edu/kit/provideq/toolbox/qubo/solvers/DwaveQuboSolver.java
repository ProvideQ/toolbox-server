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
  private static final String METHOD_SETTING_NAME = "method";
  private static final String API_TOKEN_SETTING_NAME = "dwave-token";
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

    /*
    @Override
    public List<MetaSolverSetting> getSettings() {
        return List.of(
            new Select<String>(METHOD_SETTING_NAME, "DWave Annealing Method", List.of("sim", "hybrid", "qbsolv", "direct"), "sim"),
            new Text(API_TOKEN_SETTING_NAME, "DWave API Token (required for non-sim methods)")
        );
    }*/

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver
  ) {
        /*
        String dwaveAnnealingMethod = settings.stream()
            .filter(setting -> setting.name.equals(METHOD_SETTING_NAME))
            .map(setting -> ((Select<String>) setting))
            .findFirst()
            .map(setting -> setting.selectedOption)
            .orElse("sim");

        Optional<String> dwaveToken = settings.stream()
            .filter(setting -> setting.name.equals(API_TOKEN_SETTING_NAME))
            .map(setting -> ((Text) setting))
            .findFirst()
            .map(setting -> setting.text);
        */ //TODO: Add Setting again (currently not part of our model)

    String dwaveAnnealingMethod = "sim"; //TODO: remove this again
    Optional<String> dwaveToken = Optional.empty(); //TODO: remove this again

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
