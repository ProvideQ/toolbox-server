package edu.kit.provideq.toolbox.maxCut.solvers;

import edu.kit.provideq.toolbox.ProcessRunner;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class QiskitMaxCutSolver extends MaxCutSolver{
  private final File maxCutDirectory;

  private final ResourceProvider resourceProvider;

  @Autowired
  public QiskitMaxCutSolver(
          @Value("${qiskit.directory.max-cut}") String maxCutPath,
          ResourceProvider resourceProvider) throws IOException {
    this.resourceProvider = resourceProvider;

    maxCutDirectory = resourceProvider.getResource(maxCutPath);
  }

  @Override
  public String getName() {
    return "Qiskit MaxCut";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    //TODO: assess problemData
    return problem.type() == ProblemType.MAX_CUT;
  }

  @Override
  public float getSuitability(Problem<String> problem) {
    //TODO: implement algorithm for suitability calculation
    return 1;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution) {
    Path problemFile;
    Path solutionFile;

    // Write problem file
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

    // Run Qiskit solver via console
    try {
      var processBuilder = new ProcessBuilder()
              .command(
                      "python",
                      "maxCut_qiskit.py",
                      problemFile.toAbsolutePath().toString(),
                      solutionFile.toAbsolutePath().toString().replace('\\', '/'))
              .directory(maxCutDirectory);

      var processResult = new ProcessRunner(processBuilder).run();

      if (processResult.success()) {
        solution.complete();
        solution.setSolutionData(Files.readString(solutionFile));
        return;
      }

      solution.setDebugData("Qiskit didn't complete solving MaxCut successfully" + processResult.output());
      solution.abort();
    } catch (IOException | InterruptedException e) {
      solution.setDebugData("Solving MaxCut problem via Qiskit resulted in exception: " + e.getMessage());
      solution.abort();
    }
  }
}
