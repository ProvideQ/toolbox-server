package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.IntegerSetting;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link QuboConfiguration#QUBO} solver using a Qrisps QAOA implementation.
 */
@Component
public class QrispQuboSolver extends QuboSolver {
  private static final String SETTING_MAX_NUMBER_OF_VARS = "Max Number of Variables";
  private static final int DEFAULT_MAX_NUMBER_OF_VARS = 4;

  private final ApplicationContext context;
  private final String scriptPath;

  @Autowired
  public QrispQuboSolver(
      @Value("${qrisp.directory.qubo}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(Qrisp) QAOA Solver for QUBOs";
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new IntegerSetting(
            SETTING_MAX_NUMBER_OF_VARS,
            "The maximum number of variables that can be passed to the QAOA solver"
                + " which will affect the size of the gate.",
            1,
            10,
            DEFAULT_MAX_NUMBER_OF_VARS
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

    // This value will be passed to the python script,
    // it is used to prevent denial of service issues for large simulations.
    // Default value is 4, higher values are possible but might take much longer to simulate.
    int maxNumberOfVariables = properties.<IntegerSetting>getSetting(SETTING_MAX_NUMBER_OF_VARS)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_MAX_NUMBER_OF_VARS);

    if (maxNumberOfVariables < 1) {
      return Mono.error(new IllegalArgumentException("Max number of variables must be at least 1"));
    }

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH,
            "--size-gate", String.valueOf(maxNumberOfVariables)
        )
        .withInputFile(input, "problem.lp")
        .withOutputFile("problem.bin")
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
