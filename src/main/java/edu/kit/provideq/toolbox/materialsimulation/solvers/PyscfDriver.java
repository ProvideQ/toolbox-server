package edu.kit.provideq.toolbox.materialsimulation.solvers;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.materialsimulation.ansatzes.AnsatzConfiguration;
import edu.kit.provideq.toolbox.materialsimulation.MaterialSimulationConfiguration;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.IntegerSetting;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * {@link MaterialSimulationConfiguration#MATERIAL_SIMULATION} "solver" using a PySCF implementation.
 * It creates an electronic structure problem using a PySCF driver for material simulation.
 * It allows to define the charge and spin of the molecule.
 */
@Component
public class PyscfDriver extends MaterialSimulationSolver {
  private static final SubRoutineDefinition<String, String> ANSATZ_SUBROUTINE =
      new SubRoutineDefinition<>(AnsatzConfiguration.MATERIAL_SIMULATION_ANSATZ, "Which Ansatz should be used?");
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  private static final String SETTING_CHARGE = "Charge";
  private static final int DEFAULT_CHARGE = 0;

  private static final String SETTING_SPIN = "Spin";
  private static final int DEFAULT_SPIN = 0;

  @Autowired
  public PyscfDriver(
      @Value("${path.custom.materialsimulation-driver-pyscf}") String scriptPath,
      @Value("${venv.custom.materialsimulation-driver-pyscf}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "PySCF Driver";
  }

  @Override
  public String getDescription() {
    return "Creates an Electronic Structure Problem using the Pyscf Driver "
        + "for Material Simulation. It allows to define the charge and spin of the molecule.";
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

        Optional<String> output = processResult.output();
    if (!processResult.success() || output.isEmpty()) {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.abort();
      return Mono.just(solution);
    }

    

    return subRoutineResolver.runSubRoutine(ANSATZ_SUBROUTINE, output.get())
        .publishOn(Schedulers.boundedElastic()) // avoids blocking from Files.writeString() in try/catch
        .map(subRoutineSolution -> {
          if (subRoutineSolution.getSolutionData() == null
              || subRoutineSolution.getSolutionData().isEmpty()) {
            solution.setDebugData("Subroutine did not return a valid solution.");
            solution.abort();
            return solution;
          }

          solution.setSolutionData(subRoutineSolution.getSolutionData());
          solution.complete();

          return solution;
        }
    );
  }
}

