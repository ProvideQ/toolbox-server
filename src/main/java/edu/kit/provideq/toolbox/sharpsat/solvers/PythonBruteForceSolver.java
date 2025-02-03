package edu.kit.provideq.toolbox.sharpsat.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PythonBruteForceSolver extends SharpSatSolver {
  private final String scriptPath;
  private final ApplicationContext context;


  @Autowired
  public PythonBruteForceSolver(
      @Value("${custom.script.bruteforce-sat}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  /**
   * Returns the name of the solver.
   *
   * @return name of the solver
   */
  @Override
  public String getName() {
    return "Bruteforce Exact SharpSAT solver";
  }

  /**
   * Returns a description of the solver.
   *
   * @return description of the solver
   */
  @Override
  public String getDescription() {
    return "Sat solution counter in python using naive bruteforce approach";
  }

  /**
   * Solves a given problem instance, current status and final results as well as debug information
   * is stored in the provided {@link Solution} object.
   *
   * @param input              the problem instance to solve.
   * @param subRoutineResolver interface to execute sub-routines with.
   * @param properties         properties
   * @return the {@link Solution} in which all resulting information is stored.
   */
  @Override
  public Mono<Solution<Integer>> solve(String input, SubRoutineResolver subRoutineResolver,
      SolvingProperties properties) {

    var solution = new Solution<>(this);

    DimacsCnf dimacsCnf;
    try {
      dimacsCnf = DimacsCnf.fromString(input);
      solution.setDebugData("Using CNF input: " + dimacsCnf.toString());
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
      int solutionCount = Integer.parseInt(processResult.output().orElse(""));
      solution.setSolutionData(solutionCount);
      solution.complete();
    } else {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.fail();
    }
    return Mono.just(solution);
  }
}
