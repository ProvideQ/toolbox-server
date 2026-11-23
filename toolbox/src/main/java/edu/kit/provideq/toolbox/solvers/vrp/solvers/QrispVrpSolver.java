package edu.kit.provideq.toolbox.solvers.vrp.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.tsplib.VRPFormat;
import edu.kit.provideq.formats.tsplib.VRPSolutionFormat;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.solvers.vrp.VrpConfiguration;
import edu.kit.provideq.toolbox.solvers.vrp.VrpSolutionFamily;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link VrpConfiguration#VRP} solver using Qrisp QAOA implementation.
 */
@Component
public class QrispVrpSolver extends VrpSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public QrispVrpSolver(
      @Value("${path.qrisp.vrp}") String scriptPath,
      @Value("${venv.qrisp.vrp}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Grover-based VRP Solver (Qrisp)";
  }

  @Override
  public String getDescription() {
    return "Solves a vehicle routing problem using a Grover-based Qrisp implementation.";
  }

  @Override
  public Mono<Solution<VrpSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver resolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var solution = new Solution<>(this);
    var input = convert(formattedInput, VRPFormat.INSTANCE, conversionService);

    var processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH,
            "--size-gate", "35"
        )
        .writeInputFile(input, "problem.vrp")
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(
        solution, VRPSolutionFormat.INSTANCE, output -> output));
  }
}
