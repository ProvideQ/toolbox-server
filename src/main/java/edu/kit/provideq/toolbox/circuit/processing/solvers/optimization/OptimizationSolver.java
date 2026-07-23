package edu.kit.provideq.toolbox.circuit.processing.solvers.optimization;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.circuit.processing.solvers.CircuitProcessingSolver;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolverCharacteristic;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.SelectSetting;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OptimizationSolver implements ProblemSolver<String, String> {
  private static final String SETTING_SELECT_OPTIMIZER = "Selected Optimization Pass";
  private static final OptimizationSolver.QuantumOptimizer DEFAULT_OPTIMIZER =
      QuantumOptimizer.DECOMPOSE_MULTI_CX;

  private final Map<QuantumOptimizer, String> optimizerScriptPaths;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public OptimizationSolver(
      @Value("${path.circuitprocessing.circuitoptimization.decomposemulticx}")
      String decomposeMultiCxPath,
      @Value("${path.circuitprocessing.circuitoptimization.removeredundancies}")
      String removeRedundanciesPath,
      @Value("${venv.circuitprocessing.circuitoptimization}") String venv,
      ApplicationContext context
  ) {
    this.optimizerScriptPaths = Map.of(
        QuantumOptimizer.DECOMPOSE_MULTI_CX, decomposeMultiCxPath,
        QuantumOptimizer.REMOVE_REDUNDANCIES, removeRedundanciesPath
    );
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Apply Tket Optimization Pass";
  }

  @Override
  public String getDescription() {
    return "Transform the given circuit into an optimized but equivalent circuit using"
        + "Tket compilation passes (e.g. removing redundancies).";
  }

  @Override
  public List<SolverCharacteristic> getCharacteristics() {
    return List.of(SolverCharacteristic.REFORMULATION);
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(CircuitProcessingSolver.CIRCUIT_PROCESSING_SUBROUTINE);
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new SelectSetting<>(
            SETTING_SELECT_OPTIMIZER,
            "The optimization pass to refactor the code with",
            List.of(OptimizationSolver.QuantumOptimizer.values()),
            QuantumOptimizer.DECOMPOSE_MULTI_CX,
            OptimizationSolver.QuantumOptimizer::getValue
        )
    );
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    var solution = new Solution<>(this);

    OptimizationSolver.QuantumOptimizer selectedOptimizer = properties
        .<SelectSetting<OptimizationSolver.QuantumOptimizer>>getSetting(SETTING_SELECT_OPTIMIZER)
        .map(s -> s.getSelectedOptionT(OptimizationSolver.QuantumOptimizer::fromValue))
        .orElse(DEFAULT_OPTIMIZER);

    var processResult = context
        .getBean(PythonProcessRunner.class, optimizerScriptPaths.get(selectedOptimizer), venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH
        )
        .writeInputFile(input)
        .readOutputString()
        .run(getProblemType(), solution.getId());

    if (processResult.success() && processResult.output().isPresent()) {
      return subRoutineResolver
          .runSubRoutine(CircuitProcessingSolver.CIRCUIT_PROCESSING_SUBROUTINE,
              processResult.output().get())
          .map(subRoutineSolution -> Solution.from(this, subRoutineSolution, s -> s));
    }
    solution.fail();
    processResult.errorOutput().ifPresent(solution::setDebugData);
    return Mono.just(solution);
  }

  @Override
  public ProblemType<String, String> getProblemType() {
    return OptimizationConfiguration.OPTIMIZATION_CONFIG;
  }

  enum QuantumOptimizer {
    DECOMPOSE_MULTI_CX("DecomposeMultiQubitsCX"),
    REMOVE_REDUNDANCIES("RemoveRedundancies");

    private final String value;

    QuantumOptimizer(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static OptimizationSolver.QuantumOptimizer fromValue(String value) {
      for (OptimizationSolver.QuantumOptimizer simulator : values()) {
        if (simulator.value.equals(value)) {
          return simulator;
        }
      }
      throw new IllegalArgumentException("Unknown value: " + value);
    }
  }
}
