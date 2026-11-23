package edu.kit.provideq.toolbox.solvers.sat.solvers;

import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnf;
import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnfFormat;
import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnfSolution;
import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnfSolutionFormat;
import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.GamsProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.solvers.sat.SatConfiguration;
import edu.kit.provideq.formats.sat.SatSolutionFamily;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link SatConfiguration#SAT} solver using a GAMS implementation.
 */
@Component
public class GamsSatSolver extends SatSolver {
  private final String scriptPath;
  private final ApplicationContext context;

  @Autowired
  public GamsSatSolver(
      @Value("${path.gams.sat}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "GAMS SAT";
  }

  @Override
  public String getDescription() {
    return "Solves SAT problems using GAMS as a Mixed Integer Programming problem.";
  }

  @Override
  public Mono<Solution<SatSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var solution = new Solution<>(this);
    var dimacsCnf = convert(formattedInput, DimacsCnfFormat.INSTANCE, conversionService);
    solution.setDebugData("Using cnf input: " + dimacsCnf);

    // Run SAT with GAMS via console
    ProcessResult<String> processResult = context
        .getBean(GamsProcessRunner.class, scriptPath)
        .withArguments(
            "--INPUT=" + ProcessRunner.INPUT_FILE_PATH,
            "--SOLOUTPUT=" + ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(dimacsCnf.toString())
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    if (processResult.success()) {
      DimacsCnf structuredCnf;
      try {
        structuredCnf = dimacsCnf.parse();
      } catch (ParseException e) {
        solution.setDebugData("Could not parse CNF input: " + e.getMessage());
        solution.fail();
        return Mono.just(solution);
      }

      var dimacsCnfSolution = Document.of(
          DimacsCnfSolution.fromString(structuredCnf, processResult.output().orElse("")));

      solution.setSolutionData(dimacsCnfSolution, DimacsCnfSolutionFormat.INSTANCE);
      solution.complete();
    } else {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.fail();
    }
    return Mono.just(solution);
  }
}