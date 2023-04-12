package edu.kit.provideq.toolbox.maxCut.solvers;

import edu.kit.provideq.toolbox.ProcessRunner;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CirqMaxCutSolver extends MaxCutSolver {
  private final ResourceProvider resourceProvider;
  private final File maxCutDirectory;

  @Autowired
  public CirqMaxCutSolver(
      @Value("${cirq.directory.maxCut}") String maxCutPath,
      ResourceProvider resourceProvider
  ) throws IOException {
    this.resourceProvider = resourceProvider;

    this.maxCutDirectory = resourceProvider.getResource(maxCutPath);
  }

  @Override
  public String getName() {
    return "Cirq MaxCut";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.MAX_CUT;
  }

  @Override
  public float getSuitability(Problem<String> problem) {
    // TODO: assess suitability algorithmically
    return 1;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution) {
    Path problemFile;
    Path solutionFile;

    try {
      File problemDirectory = resourceProvider.getProblemDirectory(problem, solution);

      problemFile = Paths.get(problemDirectory.getAbsolutePath(), "problem.gml");
      solutionFile = Paths.get(problemDirectory.getAbsolutePath(), "problem.sol");

      Files.writeString(problemFile, problem.problemData());
    } catch (IOException e) {
      solution.setDebugData("Creation of problem file caught exception: " + e.getMessage());
      solution.abort();
      return;
    }

    try {
      var processBuilder = new ProcessBuilder()
          .command(
              "python",
              "maxCut_cirq.py",
              problemFile.toAbsolutePath().toString(),
              // avoid windows newline characters for python
              solutionFile.toAbsolutePath().toString().replace('\\', '/')
          )
          .directory(maxCutDirectory);

      var result = new ProcessRunner(processBuilder).run();

      if (result.success()) {
        solution.complete();
        solution.setSolutionData(Files.readString(solutionFile));
      } else {
        solution.setDebugData("Cirq didn't complete solving MaxCut successfully!\n" +
            result.output());
      }
    } catch (IOException | InterruptedException e) {
      solution.setDebugData("Solving MaxCut with Cirq resulted in exception: " + e.getMessage());
      solution.abort();
    }
  }
}
