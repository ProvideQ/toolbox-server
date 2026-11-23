package edu.kit.provideq.toolbox.solvers.knapsack.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.knapsack.KnapsackFormat;
import edu.kit.provideq.formats.knapsack.KnapsackSolutionFormat;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.solvers.knapsack.KnapsackConfiguration;
import edu.kit.provideq.toolbox.solvers.knapsack.KnapsackSolutionFamily;
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
 * {@link KnapsackConfiguration#KNAPSACK} solver using a Qiskit implementation.
 */
@Component
public class QiskitKnapsackSolver extends KnapsackSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public QiskitKnapsackSolver(
      @Value("${path.qiskit.knapsack}") String scriptPath,
      @Value("${venv.qiskit.knapsack}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Qiskit Knapsack";
  }

  @Override
  public String getDescription() {
    return "Solves the knapsack problem using Qiskit with QAOA.";
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
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(knapsackInput)
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(
        solution, KnapsackSolutionFormat.INSTANCE, output -> {
          try {
            return KnapsackSolutionFormat.INSTANCE.parse(output);
          } catch (ParseException e) {
            throw new ConversionException(
                "Failed to parse the output of the Qiskit Knapsack solver.", e);
          }
        }));
  }
}
