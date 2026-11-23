package edu.kit.provideq.toolbox.solvers.knapsack.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.knapsack.KnapsackSolutionFamily;
import edu.kit.provideq.formats.knapsack.formats.KnapsackFormat;
import edu.kit.provideq.formats.knapsack.formats.KnapsackSolutionFormat;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.meta.RuleProperty;
import edu.kit.provideq.toolbox.meta.RuleType;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.meta.SolverCharacteristics;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.solvers.knapsack.KnapsackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link KnapsackConfiguration#KNAPSACK} solver using a Horowitz-Sahni implementation.
 */
@Component
public class PythonKnapsackSolver extends KnapsackSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public PythonKnapsackSolver(
          @Value("${path.custom.hs-knapsack}") String scriptPath,
          @Value("${venv.custom.hs-knapsack}") String venv,
          ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Horowitz-Sahni Knapsack";
  }

  @Override
  public String getDescription() {
    return "A solver for the Knapsack problem using the Horowitz-Sahni "
        + "branch and search algorithm.";
  }

  @Override
  public SolverCharacteristics getCharacteristics() {
    return SolverCharacteristics.of(RuleType.SOLVE, RuleProperty.EXACT, RuleProperty.VALID);
  }

  @Override
  public Mono<Solution<KnapsackSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var solution = new Solution<>(this);

    var problem = convert(formattedInput, KnapsackFormat.INSTANCE, conversionService);
    var knapsackInput = KnapsackFormat.INSTANCE.serializeToString(problem);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH)
        .writeInputFile(knapsackInput)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(
        solution, KnapsackSolutionFormat.INSTANCE, output -> {
          try {
            return KnapsackSolutionFormat.INSTANCE.parse(output);
          } catch (ParseException e) {
            throw new ConversionException(
                "Couldn't convert solver output to Knapsack solution:\n" + e, e);
          }
        }));
  }
}
