package edu.kit.provideq.toolbox.circuit.processing.solver.optimization;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.circuit.processing.solver.CircuitProcessingSolver;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.SelectSetting;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.List;
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

  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public OptimizationSolver(
      @Value("${circuitoptimizing.base.directory}") String scriptPath,
      ApplicationContext context
  ) {
    this.scriptPath = scriptPath;
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
        .getBean(PythonProcessRunner.class, scriptPath + selectedOptimizer.getScriptPath())
        .withArguments(input)
        .readOutputString()
        .run(getProblemType(), solution.getId());

    if (processResult.success() && processResult.output().isPresent()) {
      solution.complete();
      solution.setSolutionData(processResult.output().get());
      return subRoutineResolver
          .runSubRoutine(CircuitProcessingSolver.CIRCUIT_PROCESSING_SUBROUTINE,
              processResult.output().get());
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
    DECOMPOSE_MULTI_CX("DecomposeMultiQubitsCX",
        "decompose-multi-cx/decompose_multi_cx_optimizer.py"),
    REMOVE_REDUNDANCIES("RemoveRedundancies",
        "remove-redundancies/remove_redundancies_optimizer.py");

    private final String value;
    private final String scriptPath;

    QuantumOptimizer(String value, String scriptPath) {
      this.value = value;
      this.scriptPath = scriptPath;
    }

    public String getValue() {
      return value;
    }

    public String getScriptPath() {
      return scriptPath;
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
