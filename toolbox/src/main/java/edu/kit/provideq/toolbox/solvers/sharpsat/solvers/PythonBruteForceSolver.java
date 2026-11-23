package edu.kit.provideq.toolbox.solvers.sharpsat.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.sat.formats.dimacs.DimacsCnfFormat;
import edu.kit.provideq.toolbox.meta.RuleProperty;
import edu.kit.provideq.toolbox.meta.RuleType;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.meta.SolverCharacteristics;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.solvers.sharpsat.SharpSatConfiguration;
import edu.kit.provideq.toolbox.solvers.sharpsat.formats.SolutionCount;
import edu.kit.provideq.toolbox.solvers.sharpsat.formats.SolutionCountFamily;
import edu.kit.provideq.toolbox.solvers.sharpsat.formats.SolutionCountFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link SharpSatConfiguration#SHARPSAT} solver using brute-force approach.
 */
@Component
public class PythonBruteForceSolver extends SharpSatSolver {
  private final String scriptPath;
  private final String venv;
  private final ApplicationContext context;

  @Autowired
  public PythonBruteForceSolver(
      @Value("${path.custom.sharp-sat-bruteforce}") String scriptPath,
      @Value("${venv.custom.sharp-sat-bruteforce}") String venv,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.venv = venv;
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

  @Override
  public SolverCharacteristics getCharacteristics() {
    return SolverCharacteristics.of(RuleType.SOLVE, RuleProperty.EXACT, RuleProperty.VALID);
  }

  /**
   * Solves a given problem instance, current status and final results as well as debug information
   * is stored in the provided {@link Solution} object.
   *
   * @param formattedInput    the problem instance to solve.
   * @param subRoutineResolver interface to execute sub-routines with.
   * @param properties         properties
   * @return the {@link Solution} in which all resulting information is stored.
   */
  @Override
  public Mono<Solution<SolutionCountFamily>> solve(
      FormattedValue<?> formattedInput, SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var solution = new Solution<>(this);
    var dimacsCnf = convert(formattedInput, DimacsCnfFormat.INSTANCE, conversionService);
    solution.setDebugData("Using CNF input: " + dimacsCnf);

    ProcessResult<String> processResult = context
        .getBean(PythonProcessRunner.class, scriptPath, venv)
        .withArguments(
            ProcessRunner.INPUT_FILE_PATH,
            "--output-file", ProcessRunner.OUTPUT_FILE_PATH
        )
        .writeInputFile(dimacsCnf.toString())
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    if (processResult.success()) {
      try {
        var solutionCount = Integer.parseInt(processResult.output().orElse(""));
        solution.setSolutionData(new SolutionCount(solutionCount), SolutionCountFormat.INSTANCE);
        solution.complete();
      } catch (NumberFormatException e) {
        solution.setDebugData("Failed to parse solution count: " + e.getMessage());
        solution.fail();
      }
    } else {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.fail();
    }
    return Mono.just(solution);
  }
}
