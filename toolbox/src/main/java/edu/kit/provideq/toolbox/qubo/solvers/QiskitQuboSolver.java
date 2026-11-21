package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.lp.LPFormat;
import edu.kit.provideq.formats.lp.LPSolutionFormat;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import edu.kit.provideq.toolbox.qubo.QuboSolutionFamily;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link QuboConfiguration#QUBO} solver using a Qiskit QAOA implementation.
 */
@Component
public class QiskitQuboSolver extends QuboSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public QiskitQuboSolver(
      @Value("${path.qiskit.qubo}") String scriptPath,
      @Value("${venv.qiskit.qubo}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(Qiskit) QAOA Solver for QUBOs";
  }

  @Override
  public String getDescription() {
    return "Solves QUBOs using the Qiskit QAOA implementation.";
  }

  @Override
  public Mono<Solution<QuboSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var input = convert(formattedInput, LPFormat.INSTANCE, conversionService);
    var solution = new Solution<>(this);

    // Run Qiskit solver via console
    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(input, "problem.lp")
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    // Return if process failed
    return Mono.just(processResult.applyTo(
        solution, LPSolutionFormat.INSTANCE, output -> output));
  }
}
