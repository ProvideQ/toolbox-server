package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.SelectSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.TextSetting;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import java.util.List;
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
  private static final String SETTING_DWAVE_TOKEN = "D-Wave Token";
  private static final String SETTING_ANNNEALING_METHOD = "Annealing Method";
  private static final AnnealingMethod DEFAULT_ANNEALING_METHOD = AnnealingMethod.SIMULATED;

  enum AnnealingMethod {
    SIMULATED("sim"),
    HYBRID("hybrid"),
    QBSOLV("qbsolv"),
    DIRECT("direct");

    private final String value;

    AnnealingMethod(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static AnnealingMethod fromValue(String value) {
      for (AnnealingMethod method : values()) {
        if (method.value.equals(value)) {
          return method;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }

  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public DwaveQuboSolver(
      @Value("${path.dwave.qubo}") String scriptPath,
      @Value("${venv.dwave.qubo}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(D-Wave) Annealing QUBO Solver";
  }

  @Override
  public String getDescription() {
    return "Solves the QUBO problem using a D-Wave Quantum Annealer.";
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new TextSetting(
            SETTING_DWAVE_TOKEN,
            "The D-Wave token to use, needed to access the D-Wave hardware"
        ),
        new SelectSetting<>(
            SETTING_ANNNEALING_METHOD,
            "The annealing method to use, only relevant when a token is added",
            List.of(AnnealingMethod.values()),
            AnnealingMethod.SIMULATED,
            AnnealingMethod::getValue
        )
    );
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {

    Optional<String> dwaveToken = properties
        .<TextSetting>getSetting(SETTING_DWAVE_TOKEN)
        .map(TextSetting::getText);

    // this field is only relevant when a dwaveToken is added
    // (a token is needed to access the d-wave hardware)
    final var dwaveAnnealingMethod = properties
        .<SelectSetting<AnnealingMethod>>getSetting(SETTING_ANNNEALING_METHOD)
        .map(s -> s.getSelectedOptionT(AnnealingMethod::fromValue))
        .orElse(DEFAULT_ANNEALING_METHOD);

    final var solution = new Solution<>(this);

    var processRunner = context.getBean(PythonProcessRunner.class, scriptPath, venv);

    if (dwaveToken.isPresent() && !dwaveToken.get().isEmpty()) {
      processRunner.withEnvironmentVariable("DWAVE_API_TOKEN", dwaveToken.get());
    }

    // Remove "End" at the end of the input as python script can't handle it
    input = input.trim();
    if (input.endsWith("End")) {
      input = input.substring(0, input.length() - 3);
    }

    var processResult = processRunner
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            dwaveAnnealingMethod.value,
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input, "problem.lp")
        .readOutputFile("problem.bin")
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
