package edu.kit.provideq.toolbox.demonstrators;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.IntegerSetting;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CplexMipDemonstrator implements Demonstrator {
  private static final String SETTING_MAX_NUMBER_OF_VARS = "Max Number of Variables";
  private static final int DEFAULT_MAX_NUMBER_OF_VARS = 500;
  private static final String SETTING_STEP_SIZE = "Step Size";
  private static final int DEFAULT_STEP_SIZE = 20;
  private static final String SETTING_REPETITIONS = "Repetitions";
  private static final int DEFAULT_REPETITIONS = 3;

  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public CplexMipDemonstrator(
      @Value("${cplex.directory.mip}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Cplex MIP Demonstrator";
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new IntegerSetting(
            true,
            SETTING_MAX_NUMBER_OF_VARS,
            "The maximum number of variables to be used in the experiment",
            1,
            1000,
            DEFAULT_MAX_NUMBER_OF_VARS
        ),
        new IntegerSetting(
            true,
            SETTING_STEP_SIZE,
            "Interval at which the number of variables is augmented "
                + "to reach the max number of variables",
            1,
            100,
            DEFAULT_STEP_SIZE
        ),
        new IntegerSetting(
            true,
            SETTING_REPETITIONS,
            "Number of times the experiment is repeated for each number of variables",
            1,
            50,
            DEFAULT_REPETITIONS
        )
    );
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties) {
    var solution = new Solution<>(this);

    var maxNumberVariables = properties.<IntegerSetting>getSetting(SETTING_MAX_NUMBER_OF_VARS)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_MAX_NUMBER_OF_VARS);

    var stepSize = properties.<IntegerSetting>getSetting(SETTING_STEP_SIZE)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_STEP_SIZE);

    var repetitions = properties.<IntegerSetting>getSetting(SETTING_REPETITIONS)
        .map(IntegerSetting::getValue)
        .orElse(DEFAULT_REPETITIONS);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath)
        .withArguments(maxNumberVariables.toString(), stepSize.toString(), repetitions.toString())
        .withOutputString()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(solution));
  }
}
