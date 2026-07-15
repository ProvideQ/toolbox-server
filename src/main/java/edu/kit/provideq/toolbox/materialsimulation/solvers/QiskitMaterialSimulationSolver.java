package edu.kit.provideq.toolbox.materialsimulation.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.materialsimulation.MaterialSimulationConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.IntegerSetting;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.List;
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

  private static final String SETTING_CHARGE = "Charge";
  private static final int DEFAULT_CHARGE = 0;

  private static final String SETTING_SPIN = "Spin";
  private static final int DEFAULT_SPIN = 0;

  @Autowired
  public QiskitMaterialSimulationSolver(
      @Value("${path.qiskit.materialsimulation}") String scriptPath,
      @Value("${venv.qiskit.materialsimulation}") String venv,
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
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new IntegerSetting(
            SETTING_CHARGE,
            "The charge of the molecule. The parameter to define "
                + "the total number of electrons in the system.",
            DEFAULT_CHARGE,
            0, Integer.MAX_VALUE
        ),
        new IntegerSetting(
            SETTING_SPIN,
            "The spin of the molecule. Equals the number of unpaired electrons 2S,"
                + " i.e. the difference between the number of alpha and beta electrons.",
            DEFAULT_SPIN,
            0, Integer.MAX_VALUE
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

    int charge = properties.<IntegerSetting>getSetting(SETTING_CHARGE)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_CHARGE);

    int spin = properties.<IntegerSetting>getSetting(SETTING_SPIN)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_SPIN);

    if (charge < 0) {
      throw new IllegalArgumentException("Charge must be non-negative");
    }
    if (spin < 0) {
      throw new IllegalArgumentException("Spin must be non-negative");
    }

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH,
            "--charge", String.valueOf(charge),
            "--spin", String.valueOf(spin)
        )
        .writeInputFile(input)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
