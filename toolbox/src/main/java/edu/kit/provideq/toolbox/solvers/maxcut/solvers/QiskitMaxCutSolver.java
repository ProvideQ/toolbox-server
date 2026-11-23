package edu.kit.provideq.toolbox.solvers.maxcut.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.gml.GmlFormat;
import edu.kit.provideq.formats.gml.GmlSolutionFormat;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.solvers.maxcut.MaxCutConfiguration;
import edu.kit.provideq.toolbox.solvers.maxcut.MaxCutSolutionFamily;
import edu.kit.provideq.toolbox.solvers.maxcut.QaoaMaxCutReportFormat;
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
 * {@link MaxCutConfiguration#MAX_CUT} solver using a Qiskit implementation.
 */
@Component
public class QiskitMaxCutSolver extends MaxCutSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public QiskitMaxCutSolver(
      @Value("${path.qiskit.max-cut}") String scriptPath,
      @Value("${venv.qiskit.max-cut}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Qiskit MaxCut";
  }

  @Override
  public String getDescription() {
    return "Solves the MaxCut problem using a Qiskit implementation with a "
        + "Variational Quantum Eigensolver.";
  }

  @Override
  public Mono<Solution<MaxCutSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var gmlDocument = convert(formattedInput, GmlFormat.INSTANCE, conversionService);
    var solution = new Solution<>(this);

    // Run Qiskit solver via console
    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(gmlDocument.toString())
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    // Return if process failed
    if (!processResult.success() || processResult.output().isEmpty()) {
      return Mono.just(processResult.applyTo(
          solution, QaoaMaxCutReportFormat.INSTANCE, output -> output));
    }

    // Convert the QAOA report into a GML solution annotated with node partitions
    // the conversion is context-bound to the problem graph (bits assign partitions by node order)
    try {
      var gmlSolution = conversionService.convert(
          processResult.output().get(),
          QaoaMaxCutReportFormat.INSTANCE,
          GmlSolutionFormat.INSTANCE,
          gmlDocument.parse());

      solution.setSolutionData(gmlSolution, GmlSolutionFormat.INSTANCE);
      solution.complete();
    } catch (ParseException e) {
      throw new ConversionException("Couldn't convert problem data to GML:\n" + e, e);
    } catch (FormatException e) {
      solution.setDebugData("Could not convert QAOA report to GML solution: " + e.getMessage());
      solution.fail();
    }
    return Mono.just(solution);
  }
}
