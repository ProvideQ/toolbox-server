package edu.kit.provideq.toolbox.demonstrators;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.TextSetting;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Demonstrator for the Molecule Energy simulation.
 * Note that its python dependencies can only be installed on Linux and macOS.
 * Based on this <a href="https://github.com/qiskit-community/qiskit-community-tutorials/blob/master/chemistry/h2_var_forms.ipynb">Jupyter Notebook</a>
 */
@Component
public class MoleculeEnergySimulator implements Demonstrator {
  private final String scriptPath;
  private final ApplicationContext context;

  private static final String SETTING_MOLECULE = "Molecule";
  private static final String DEFAULT_MOLECULE = "H .0 .0 .0; H .0 .0 0.74279";

  @Autowired
  public MoleculeEnergySimulator(
      @Value("${demonstrators.qiskit.script.molecule-energy}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Molecule Energy Simulator";
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new TextSetting(
            false,
            SETTING_MOLECULE,
            "The molecule to be simulated in XYZ format - a di-hydrogen molecule by default",
            DEFAULT_MOLECULE
        )
    );
  }

  @Override
  public Mono<Solution<String>> solve(String input, SubRoutineResolver subRoutineResolver,
                                      SolvingProperties properties) {
    var solution = new Solution<>(this);

    var molecule = properties.<TextSetting>getSetting(SETTING_MOLECULE)
        .map(TextSetting::getText)
        .orElse(DEFAULT_MOLECULE);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath)
        .withArguments(molecule)
        .readOutputString()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
