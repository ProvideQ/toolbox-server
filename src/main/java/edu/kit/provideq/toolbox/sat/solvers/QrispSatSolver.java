package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.sat.SatConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link SatConfiguration#SAT} solver using QRISP implementation.
 */
@Component
public class QrispSatSolver extends SatSolver {
  private final String scriptPath;
  private final ApplicationContext context;

  public QrispSatSolver(
      @Value("${qrisp.script.sat}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "QRISP SAT";
  }

  @Override
  public Mono<Solution<DimacsCnfSolution>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
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

    ProcessResult<String> processResult = context
        .getBean(PythonProcessRunner.class, scriptPath)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(dimacsCnf.toString())
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    if (processResult.success()) {
      var dimacsCnfSolution =
          DimacsCnfSolution.fromString(dimacsCnf, processResult.output().orElse(""));

      solution.setSolutionData(dimacsCnfSolution);
      solution.complete();
    } else {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.fail();
    }
    return Mono.just(solution);
  }
}