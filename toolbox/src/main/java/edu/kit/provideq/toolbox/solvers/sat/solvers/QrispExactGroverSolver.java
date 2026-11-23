package edu.kit.provideq.toolbox.solvers.sat.solvers;

import edu.kit.provideq.formats.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfFormat;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfSolutionFormat;
import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.solvers.sat.SatConfiguration;
import edu.kit.provideq.toolbox.solvers.sat.SatFamily;
import edu.kit.provideq.toolbox.solvers.sat.SatSolutionFamily;
import edu.kit.provideq.toolbox.solvers.sharpsat.SharpSatConfiguration;
import edu.kit.provideq.toolbox.solvers.sharpsat.SolutionCountFamily;
import edu.kit.provideq.toolbox.solvers.sharpsat.SolutionCountFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * {@link SatConfiguration#SAT} solver using QRISP-Grover implementation.
 */
@Component
public class QrispExactGroverSolver extends SatSolver {

  private static final SubRoutineDefinition<SatFamily, SolutionCountFamily> SHARPSAT_SUBROUTINE =
      SubRoutineDefinition.of(
          SharpSatConfiguration.SHARPSAT,
          "Count the number of solutions using a SharpSAT solver",
          true
      );

  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  public QrispExactGroverSolver(
      @Value("${path.qrisp.sat.exact}") String scriptPath,
      @Value("${venv.qrisp.sat}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Exact QRISP SAT";
  }

  @Override
  public String getDescription() {
    return "Solves SAT problems using QRISP as a quantum Grover-search problem. "
        + "It measures only possible states by utilizing result of solution numbers "
        + "from the SharpSAT solver.";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(SHARPSAT_SUBROUTINE);
  }

  @Override
  public Mono<Solution<SatSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var solution = new Solution<>(this);
    var dimacsCnf = convertFormatted(formattedInput, DimacsCnfFormat.INSTANCE, conversionService);
    solution.setDebugData("Using CNF input: " + dimacsCnf);
    return subRoutineResolver.runSubRoutine(SHARPSAT_SUBROUTINE, dimacsCnf, conversionService)
        .publishOn(Schedulers.boundedElastic())
        .flatMap(sharpSatSolution ->
            processSharpSatResult(
                sharpSatSolution,
                dimacsCnf.getValue(),
                solution,
                conversionService));
  }

  private Mono<Solution<SatSolutionFamily>> processSharpSatResult(
      Solution<SolutionCountFamily> sharpSatSolution,
      Document<DimacsCnf> dimacsCnf,
      Solution<SatSolutionFamily> satSolution,
      ConversionService conversionService) {
    var solutionCount = convert(
        sharpSatSolution.getSolutionData(),
        SolutionCountFormat.INSTANCE,
        conversionService);

    satSolution.setDebugData("Sharpsat subroutine found " + solutionCount.count() + " solutions.");
    return runPythonSolver(dimacsCnf, solutionCount.count(), satSolution);
  }

  private Mono<Solution<SatSolutionFamily>> runPythonSolver(
      Document<DimacsCnf> dimacsCnf,
      long solutionCount,
      Solution<SatSolutionFamily> solution
  ) {
    ProcessResult<String> processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            "--solution-count", String.valueOf(solutionCount),
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(dimacsCnf.toString())
        .readOutputFile()
        .run(getProblemType(), solution.getId());
    if (processResult.success()) {
      DimacsCnf cnf;
      try {
        cnf = dimacsCnf.parse();
      } catch (ParseException e) {
        solution.setDebugData("Could not parse CNF input: " + e.getMessage());
        solution.fail();
        return Mono.just(solution);
      }

      var dimacsCnfSolution = Document.of(
          DimacsCnfSolution.fromString(
              cnf,
              processResult.output().orElse("")
          )
      );
      solution.setSolutionData(dimacsCnfSolution, DimacsCnfSolutionFormat.INSTANCE);
      solution.complete();
    } else {
      solution.setDebugData(processResult.errorOutput().orElse("Error occurred while running"
          + " Qrisp implementation of Grover "));
      solution.fail();
    }
    return Mono.just(solution);
  }
}