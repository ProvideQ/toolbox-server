package edu.kit.provideq.toolbox.circuit.processing.solver.executor;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.IntegerSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.SelectSetting;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ExecutionSolver implements ProblemSolver<String, ExecutionResult> {
  private static final String SETTING_NUMBER_OF_SHOTS = "Number of shots";
  private static final String SETTING_SELECT_SIMULATOR = "Selected Simulator";
  private static final int DEFAULT_NUMBER_OF_SHOTS = 1024;
  private static final QuantumSimulator DEFAULT_SIMULATOR = QuantumSimulator.AER;

  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public ExecutionSolver(
      @Value("${path.circuitprocessing.circuitexecution}") String scriptPath,
      @Value("${venv.circuitprocessing.circuitexecution}") String venv,
      ApplicationContext context
  ) {
    this.context = context;
    this.venv = venv;
    this.scriptPath = scriptPath;
  }

  @Override
  public String getName() {
    return "Execute OpenQASM circuit";
  }

  @Override
  public String getDescription() {
    return "Execute an OpenQASM circuit";
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new IntegerSetting(
            SETTING_NUMBER_OF_SHOTS,
            "The number of shots to run",
            1,
            1000000,
            DEFAULT_NUMBER_OF_SHOTS),
        new SelectSetting<>(
            SETTING_SELECT_SIMULATOR,
            "The simulator to run the code with",
            List.of(QuantumSimulator.values()),
            QuantumSimulator.AER,
            QuantumSimulator::getValue
        )
    );
  }

  @Override
  public Mono<Solution<ExecutionResult>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    int shotNumber = properties.<IntegerSetting>getSetting(SETTING_NUMBER_OF_SHOTS)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_NUMBER_OF_SHOTS);

    QuantumSimulator selectedSimulator = properties
        .<SelectSetting<QuantumSimulator>>getSetting(SETTING_SELECT_SIMULATOR)
        .map(s -> s.getSelectedOptionT(QuantumSimulator::fromValue))
        .orElse(DEFAULT_SIMULATOR);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath + "executor.py", venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            String.valueOf(shotNumber),
            selectedSimulator.getBackendKey()
        )
        .writeInputFile(input)
        .readOutputString()
        .run(getProblemType(), solution.getId());

    if (processResult.success()) {
      solution.complete();
      solution.setSolutionData(new ExecutionResult(processResult.output(), Optional.of(input)));
      return Mono.just(solution);
    }
    solution.fail();
    processResult.errorOutput().ifPresent(solution::setDebugData);
    return Mono.just(solution);
  }

  @Override
  public ProblemType<String, ExecutionResult> getProblemType() {
    return ExecutorConfiguration.EXECUTOR_CONFIG;
  }

  enum QuantumSimulator {
    AER("AerBackend", "aer"),
    // PROJECTQ("ProjectQBackend", "projectq"),
    QULACS("QulacsBackend", "qulacs"),
    AER_NOISY("Aer Noisy Backend (max. 2 qubits)", "aer_noisy");

    private final String value;
    private final String backendKey;

    QuantumSimulator(String value, String backendKey) {
      this.value = value;
      this.backendKey = backendKey;
    }

    public String getValue() {
      return value;
    }

    public String getBackendKey() {
      return backendKey;
    }

    public static QuantumSimulator fromValue(String value) {
      for (QuantumSimulator simulator : values()) {
        if (simulator.value.equals(value)) {
          return simulator;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
