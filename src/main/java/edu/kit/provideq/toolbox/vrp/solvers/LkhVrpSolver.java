package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.formats.VRPFormat;
import edu.kit.provideq.toolbox.formats.VRPSolutionFormat;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.vrp.VrpConfiguration;
import edu.kit.provideq.toolbox.vrp.VrpSolutionFamily;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link VrpConfiguration#VRP} classical solver using the LKH-3 heuristic.
 */
@Component
public class LkhVrpSolver extends VrpSolver {
  private final String scriptPath;
  private final String binaryPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public LkhVrpSolver(
      @Value("${path.custom.lkh}") String scriptPath,
      @Value("${path.custom.lkh.binary}") String binaryPath,
      @Value("${venv.custom.lkh}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.binaryPath = binaryPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "LKH-3 VRP Solver";
  }

  @Override
  public String getDescription() {
    return "Solves a vehicle routing problem using the LKH-3 heuristic.";
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
            "--lkh-instance", binaryPath,
            ProcessRunner.INPUT_FILE_PATH,
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(adaptInput(input), "problem.vrp")
        .readOutputFile("problem.sol")
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(
        solution, VRPSolutionFormat.INSTANCE, output -> output));
  }

  /**
   * LKH-3 solver has an issue when the "EOF" tag.
   * This method removes this substring.
   *
   * @param originalInput original input of the TSP problem
   * @return adapted input with "EOF"
   */
  private String adaptInput(String originalInput) {
    String inputAsVrp = originalInput;
    if (inputAsVrp.endsWith("EOF")) {
      inputAsVrp = inputAsVrp.replaceAll("EOF$", "");
    }
    return inputAsVrp;
  }
}