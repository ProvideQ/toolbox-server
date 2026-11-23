package edu.kit.provideq.toolbox.solvers.maxcut.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.gml.GmlFormat;
import edu.kit.provideq.formats.gml.GmlSolutionFormat;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.solvers.maxcut.MaxCutConfiguration;
import edu.kit.provideq.toolbox.solvers.maxcut.MaxCutSolutionFamily;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.GamsProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link MaxCutConfiguration#MAX_CUT} solver using a GAMS implementation.
 */
@Component
public class GamsMaxCutSolver extends MaxCutSolver {
  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public GamsMaxCutSolver(
      @Value("${path.gams.max-cut}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "GAMS MaxCut";
  }

  @Override
  public String getDescription() {
    return "Solves the MaxCut problem using GAMS. This starts from a SDP relaxation then used as a "
        + " starting point for the QUBO algorithm.";
  }

  @Override
  public Mono<Solution<MaxCutSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var gml = convert(formattedInput, GmlFormat.INSTANCE, conversionService);
    var solution = new Solution<>(this);

    // Run MaxCut with GAMS via console
    var processResult = context
        .getBean(GamsProcessRunner.class, scriptPath)
        .withArguments(
            "--INPUT=" + ProcessRunner.INPUT_FILE_PATH,
            "--OUTPUT=" + ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(gml.toString())
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    return Mono.just(processResult.applyTo(
        solution, GmlSolutionFormat.INSTANCE, output -> output));
  }
}