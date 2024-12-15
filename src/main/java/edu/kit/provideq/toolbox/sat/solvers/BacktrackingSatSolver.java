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
 * {@link SatConfiguration#SAT} solver using Backtracking implementation.
 */
@Component
public class BacktrackingSatSolver extends SatSolver {
  private final String scriptPath;
  private final ApplicationContext context;

  public BacktrackingSatSolver(
      @Value("${backtracking.script.sat}") String scriptPath,
      ApplicationContext context) {
    this.scriptPath = scriptPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Backtracking SAT";
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
            "True"
        )
        .writeInputFile(dimacsCnf.toString())
        .readOutputFile()
        .run(getProblemType(), solution.getId());

    if (processResult.success()) {
      try {
        DimacsCnfSolution dimacsCnfSolution = parseSolverOutput(processResult.output()
            .orElse(""), dimacsCnf);
        solution.setSolutionData(dimacsCnfSolution);
        solution.complete();
      } catch (Exception e) {
        solution.setDebugData("Error parsing SAT solver output: " + e.getMessage());
        solution.fail();
      }
    } else {
      solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
      solution.fail();
    }

    return Mono.just(solution);
  }

  /**
   * Parses the output of the SAT solver and extracts the solution.
   *
   * @param output    The raw output from the SAT solver.
   * @param dimacsCnf The original CNF formula for context.
   * @return A DimacsCnfSolution object representing the SAT solver's result.
   * @throws RuntimeException if parsing fails or the output is invalid.
   */
  private DimacsCnfSolution parseSolverOutput(String output, DimacsCnf dimacsCnf) {
    String[] lines = output.split("\n");

    for (String line : lines) {
      if (line.trim().equals("UNSAT")) {
        return DimacsCnfSolution.fromString(dimacsCnf, "UNSAT");
      }
    }

    for (String line : lines) {
      if (line.startsWith("SAT Answer:")) {
        String answer = line.replace("SAT Answer:", "").trim();

        // handle single True/False or array format [True, False, ...]
        answer = answer.replace("[", "").replace("]", "");
        String[] values = answer.split(",");
        StringBuilder literalsBuilder = new StringBuilder();

        // convert True/False into variables
        for (int i = 0; i < values.length; i++) {
          String value = values[i].trim();
          int variable = i + 1; // variable index starts at 1
          if (value.equalsIgnoreCase("True")) {
            literalsBuilder.append(variable).append(" ");
          } else if (value.equalsIgnoreCase("False")) {
            literalsBuilder.append(-variable).append(" ");
          } else {
            throw new RuntimeException("Invalid SAT Answer format: " + value);
          }
        }

        String literals = literalsBuilder.toString().trim();
        return DimacsCnfSolution.fromString(dimacsCnf, literals);
      }
    }
    throw new RuntimeException("SAT solver output is invalid or unsupported.");
  }

}
