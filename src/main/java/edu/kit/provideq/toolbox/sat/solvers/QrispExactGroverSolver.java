package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.sat.SatConfiguration;
import edu.kit.provideq.toolbox.sharpsat.SharpSatConfiguration;
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

  private static final SubRoutineDefinition<String, Integer> SHARPSAT_SUBROUTINE =
      new SubRoutineDefinition<>(
          SharpSatConfiguration.SHARPSAT,
          "Count the number of solutions using a SharpSAT solver"
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
  public Mono<Solution<DimacsCnfSolution>> solve(
      String input, SubRoutineResolver subRoutineResolver, SolvingProperties properties
  ) {
    var solution = new Solution<>(this);
    DimacsCnf dimacsCnf;
    try {
      dimacsCnf = DimacsCnf.fromString(input);
      solution.setDebugData("Using CNF input: " + dimacsCnf);
    } catch (ConversionException | RuntimeException e) {
      solution.setDebugData("Parsing error: " + e.getMessage());
      solution.abort();
      return Mono.just(solution);
    }
    return subRoutineResolver.runSubRoutine(SHARPSAT_SUBROUTINE, dimacsCnf.toString())
        .publishOn(Schedulers.boundedElastic())
        .flatMap(sharpSatSolution -> processSharpSatResult(sharpSatSolution, dimacsCnf, solution));
  }

  private Mono<Solution<DimacsCnfSolution>> processSharpSatResult(
      Solution<Integer> sharpSatSolution,
      DimacsCnf dimacsCnf,
      Solution<DimacsCnfSolution> solution
  ) {
    if (sharpSatSolution.getSolutionData() == null) {
      solution.setDebugData("Sharpsat subroutine returned no solution data.");
      solution.abort();
      return Mono.just(solution);
    }
    int solutionCount = sharpSatSolution.getSolutionData();
    solution.setDebugData("Sharpsat subroutine found " + solutionCount + " solutions.");
    return runPythonSolver(dimacsCnf, solutionCount, solution);
  }

  private Mono<Solution<DimacsCnfSolution>> runPythonSolver(
      DimacsCnf dimacsCnf,
      int solutionCount,
      Solution<DimacsCnfSolution> solution
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
      var dimacsCnfSolution = DimacsCnfSolution.fromString(
          dimacsCnf,
          processResult.output().orElse("")
      );
      solution.setSolutionData(dimacsCnfSolution);
      solution.complete();
    } else {
      solution.setDebugData(processResult.errorOutput().orElse("Error occurred while running"
          + " Qrisp implementation of Grover "));
      solution.fail();
    }
    return Mono.just(solution);
  }
}
