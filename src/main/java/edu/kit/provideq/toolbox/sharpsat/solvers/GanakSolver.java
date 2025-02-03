package edu.kit.provideq.toolbox.sharpsat.solvers;

import static edu.kit.provideq.toolbox.process.ProcessRunner.INPUT_FILE_PATH;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.DefaultProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.sharpsat.SharpSatConfiguration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link SharpSatConfiguration#SHARPSAT} solver using OpenSource GANAK implementation.
 */
@Component
public class GanakSolver extends SharpSatSolver {
  private final String binaryPath;
  private final ApplicationContext context;

  @Autowired
  public GanakSolver(
      @Value("${custom.binary.ganak-sat}") String binaryPath,
      ApplicationContext context) {

    this.binaryPath = binaryPath;
    this.context = context;
  }

  /**
   * Returns the name of the solver.
   *
   * @return name of the solver
   */
  @Override
  public String getName() {
    return "Ganak SharpSAT solution counter";
  }

  /**
   * Returns a description of the solver.
   *
   * @return description of the solver
   */
  @Override
  public String getDescription() {
    return "Exact model counter that uses caching to calculate"
        + " the number of solutions for SAT problems.";
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

    if (binaryPath == null || binaryPath.isEmpty()) {
      throw new IllegalArgumentException("Property 'custom.binary.ganak-sat' is not defined."
          + " This solver isn't available for windows. Use PythonBruteForce instead");
    }

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
        .getBean(DefaultProcessRunner.class)
        .withArguments(binaryPath, INPUT_FILE_PATH)
        .writeInputFile(dimacsCnf.toString(), "cnf_input.cnf")
        .readOutputString()
        .run(getProblemType(), solution.getId());

    if (processResult.success()) {
      try {
        int solutionCount = parseSolutionCount(processResult.output().orElse("").trim());
        solution.setSolutionData(solutionCount);
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

  private int parseSolutionCount(String output) {
    // match lines like "s mc [number]"
    String regex = "^s mc (\\d+)$";
    Pattern pattern = Pattern.compile(regex);

    String[] lines = output.split("\\R"); // matches any line break
    for (String line : lines) {
      Matcher matcher = pattern.matcher(line);
      if (matcher.find()) {
        // extract the number
        return Integer.parseInt(matcher.group(1));
      }
    }

    throw new IllegalArgumentException("Output does not contain a valid 's mc' line.");
  }

}
