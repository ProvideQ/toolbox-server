package edu.kit.provideq.toolbox.solvers.tsp.solvers;

import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.lp.QuboFamily;
import edu.kit.provideq.formats.lp.QuboSolutionFamily;
import edu.kit.provideq.formats.lp.formats.LPFormat;
import edu.kit.provideq.formats.lp.formats.LPSolutionFormat;
import edu.kit.provideq.formats.tsplib.TspSolutionFamily;
import edu.kit.provideq.formats.tsplib.formats.TSPLIBFormat;
import edu.kit.provideq.formats.tsplib.formats.TSPLIBSolutionFormat;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.RuleProperty;
import edu.kit.provideq.toolbox.meta.RuleType;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.meta.SolverCharacteristics;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.guard.Guard;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.process.DefaultProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.solvers.qubo.QuboConfiguration;
import edu.kit.provideq.toolbox.solvers.qubo.formats.PositionalBitsSolutionFormat;
import edu.kit.provideq.toolbox.solvers.tsp.TspSolutionGuard;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Transforms TSP Problems into QUBOs.
 */
@Component
public class QuboTspSolver extends TspSolver {
  private static final SubRoutineDefinition<QuboFamily, QuboSolutionFamily> QUBO_SUBROUTINE =
      SubRoutineDefinition.of(
          QuboConfiguration.QUBO,
          "How should the QUBO be solved?",
          true
      );

  private static final boolean DEFAULT_GUARDED = false;
  private static final int DEFAULT_GUARD_RETRIES = 3;

  private final ApplicationContext context;
  private final String binaryPath;
  private ResourceProvider resourceProvider;

  @Autowired
  public QuboTspSolver(
      @Value("${path.custom.berger-vrp}") String binaryPath,
      ApplicationContext context) {
    this.binaryPath = binaryPath;
    this.context = context;
  }

  @Autowired
  public void setResourceProvider(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  @Override
  public String getName() {
    return "TSP to QUBO Transformation";
  }

  @Override
  public String getDescription() {
    return "Solves TSP Problems by transforming them into QUBOs and solving them "
        + "with a QUBO solver.";
  }

  @Override
  public SolverCharacteristics getCharacteristics() {
    return SolverCharacteristics.of(
        RuleType.REFORMULATION,
        RuleProperty.WEAKLY_CONSTRAINT_PRESERVING,
        RuleProperty.OPTIMAL_SOLUTION_PRESERVING);
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(QUBO_SUBROUTINE);
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return Guard.createSettings(
        "Check that the reconstructed tour visits every node exactly once. "
            + "Invalid or missing tours are rejected instead of being returned.",
        DEFAULT_GUARDED,
        DEFAULT_GUARD_RETRIES);
  }

  private String getProblemDirectory(Solution<TspSolutionFamily> solutionObject) {
    // write solution to current problem directory
    String problemDirectoryPath = null;
    try {
      problemDirectoryPath = resourceProvider
          .getProblemDirectory(getProblemType(), solutionObject.getId())
          .getAbsolutePath();
    } catch (IOException e) {
      solutionObject.setDebugData("Failed to retrieve problem directory.");
      solutionObject.abort();
    }
    return problemDirectoryPath;
  }

  @Override
  public Mono<Solution<TspSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver resolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    var guardConfiguration =
        Guard.readConfiguration(properties, DEFAULT_GUARDED, DEFAULT_GUARD_RETRIES);

    var solution = new Solution<>(this);
    var tspLibString = convert(formattedInput, TSPLIBFormat.INSTANCE, conversionService);

    // change "TYPE" keyword from "TSP" to "CVRP"
    // add capacity declaration of "0" (is ignored later)
    // this is theoretically wrong, but needed for Lucas' QUBO converter to work
    String typeRegex = "(?i)\\btype\\s*:\\s*tsp\\b";
    String vrpInput = tspLibString.replaceAll(typeRegex, "TYPE : CVRP\nCAPACITY : 0");

    // translate into qubo in lp-file format with rust vrp meta solver
    var processResult = context
        .getBean(DefaultProcessRunner.class)
        .withArguments(
            binaryPath,
            "partial",
            "solve", ProcessRunner.INPUT_FILE_PATH,
            "simulated",
            "--transform-only"
        )
        .writeInputFile(vrpInput, "problem.vrp")
        .readOutputFile("problem.lp")
        .run(getProblemType(), solution.getId());

    Optional<String> output = processResult.output();
    if (!processResult.success() || output.isEmpty()) {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.abort();
      return Mono.just(solution);
    }

    String problemDirectoryPath = getProblemDirectory(solution);
    if (problemDirectoryPath == null) {
      solution.setDebugData("Unable to solve Problem, the problemDirectoryPath is null.");
      solution.abort();
      return Mono.just(solution);
    }
    Path quboSolutionFilePath = Path.of(problemDirectoryPath, "problem.bin");

    return Guard.solve(
        solution,
        vrpInput,
        TspSolutionGuard.INSTANCE,
        guardConfiguration,
        text -> FormattedValue.of(text, TSPLIBSolutionFormat.INSTANCE),
        () -> solveAndReconstruct(
            resolver, conversionService, output.get(), vrpInput, quboSolutionFilePath,
            solution.getId()));
  }

  /**
   * Runs the QUBO sub-routine, converts its solution into the positional-bits representation
   * expected by the pipeline binary, and re-transforms the QUBO solution back into a tour.
   * Failures are reported as {@link Guard.Attempt}s so the guard can retry the sub-routine.
   */
  private Mono<Guard.Attempt<String>> solveAndReconstruct(
      SubRoutineResolver resolver,
      ConversionService conversionService,
      String lpProblem,
      String vrpInput,
      Path quboSolutionFilePath,
      UUID solutionId
  ) {
    return resolver.runSubRoutine(QUBO_SUBROUTINE,
            FormattedValue.of(lpProblem, LPFormat.INSTANCE), conversionService)
        .publishOn(Schedulers.boundedElastic()) //avoids block from Files.writeString() in try/catch
        .map(quboSolution -> {
          var lpSolution = convert(
              quboSolution.getSolutionData(), LPSolutionFormat.INSTANCE, conversionService);

          // the pipeline binary expects the QUBO solution as positional bits ordered by the
          // variables declared in the LP file's "Binary" section; the conversion is
          // context-bound to the LP problem text
          String positionalSolution;
          try {
            positionalSolution = conversionService.convert(
                lpSolution,
                LPSolutionFormat.INSTANCE,
                PositionalBitsSolutionFormat.INSTANCE,
                lpProblem);
          } catch (FormatException e) {
            return Guard.Attempt.failed(
                "Could not convert QUBO solution to positional bits: " + e.getMessage(),
                quboSolution);
          }

          try {
            Files.writeString(quboSolutionFilePath, positionalSolution);
          } catch (IOException e) {
            return Guard.Attempt.failed(
                "Failed to write qubo solution file with path: " + quboSolutionFilePath,
                quboSolution);
          }

          var processRetransformResult = context
              .getBean(DefaultProcessRunner.class)
              .withArguments(
                  binaryPath,
                  "partial",
                  "solve", ProcessRunner.INPUT_FILE_PATH,
                  "simulated",
                  "--qubo-solution", quboSolutionFilePath.toString()
              )
              .writeInputFile(vrpInput, "problem.vrp")
              .readOutputFile("problem.sol")
              .run(getProblemType(), solutionId);

          if (!processRetransformResult.success()) {
            return Guard.Attempt.failed(
                processRetransformResult.errorOutput().orElse("Unable to retransform result."),
                quboSolution);
          }

          return Guard.Attempt.of(
              processRetransformResult.output().orElse("Empty Solution"), quboSolution);
        });
  }
}
