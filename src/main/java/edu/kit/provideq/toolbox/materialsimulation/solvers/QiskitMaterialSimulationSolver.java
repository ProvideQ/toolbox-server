package edu.kit.provideq.toolbox.materialsimulation.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.materialsimulation.MaterialSimulationConfiguration;
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
 * {@link MaterialSimulationConfiguration#MATERIAL_SIMULATION} solver using a Qiskit
 * implementation.
 */
@Component
public class QiskitMaterialSimulationSolver extends MaterialSimulationSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public QiskitMaterialSimulationSolver(
      @Value("$path.qiskit.materialsimulation") String scriptPath,
      @Value("$venv.qiskit.materialsimulation") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Qiskit Material Simulation";
  }

  @Override
  public String getDescription() {
    return "Solves the material simulation problem using Qiskit with VQE.";
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
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
