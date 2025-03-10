package edu.kit.provideq.toolbox.circuit.processing.solver.executor;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.IntegerSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.SelectSetting;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class ExecutionSolver implements ProblemSolver<String, ExecutionResult> {
  private static final String SETTING_NUMBER_OF_SHOTS = "Number of shots";
  private static final String SETTING_SELECT_SIMULATOR = "Selected Simulator";
  private static final int DEFAULT_NUMBER_OF_SHOTS = 1024;
  private static final QuantumSimulator DEFAULT_SIMULATOR = QuantumSimulator.AER;

  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public ExecutionSolver(
      @Value("${circuitprocessing.base.directory}") String scriptPath,
      ApplicationContext context
  ) {
    this.context = context;
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
            10000,
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
  public Mono<Solution<ExecutionResult>> solve(String input, SubRoutineResolver subRoutineResolver, SolvingProperties properties) {
    var solution = new Solution<>(this);

    int shotNumber = properties.<IntegerSetting>getSetting(SETTING_NUMBER_OF_SHOTS)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_NUMBER_OF_SHOTS);

    QuantumSimulator selectedSimulator = properties
        .<SelectSetting<QuantumSimulator>>getSetting(SETTING_SELECT_SIMULATOR)
        .map(s -> s.getSelectedOptionT(QuantumSimulator::fromValue))
        .orElse(DEFAULT_SIMULATOR);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath + selectedSimulator.getScriptPath())
        .withArguments(input, String.valueOf(shotNumber))
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
    AER("AerBackend", "default-executor/default_executor.py"),
    // PROJECTQ("ProjectQBackend", "projectq-executor/projectq_executor.py"),
    QULACS("QulacsBackend", "qulacs-executor/qulacs_executor.py"),
    AER_NOISY("Aer Noisy Backend (max. 2 qubits)", "aer-noisy/aer_noisy_executor.py");

    private final String value;
    private final String scriptPath;

    QuantumSimulator(String value, String scriptPath) {
      this.value = value;
      this.scriptPath = scriptPath;
    }

    public String getValue() {
      return value;
    }

    public String getScriptPath() {
      return scriptPath;
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
