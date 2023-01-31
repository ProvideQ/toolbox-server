package edu.kit.provideq.toolbox.maxCut.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QiskitMaxCutSolver extends MaxCutSolver{
  private final File qiskitDirectory = new File(System.getProperty("user.dir"), "qiskit");
  private final File maxCutDirectory = new File(qiskitDirectory, "maxCut");
  private final File workingDirectory = new File(System.getProperty("user.dir"), "jobs");//todo move working directory to config

  private final String problemPath = "problem.gml";

  private final String solutionPath = "problem.sol";

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

    Path dir = Paths.get(workingDirectory.toString(), "qiskit", String.valueOf(solution.id()));
    Path problemFile = Paths.get(dir.toString(), problemPath);
    Path solutionFile = Paths.get(dir.toString(), solutionPath);

    //Write problem file
    try {
      Files.createDirectories(dir);
      Files.writeString(problemFile, problem.problemData());
    } catch (IOException e) {
      solution.setDebugData("Creation of problem file caught exception: " + e.getMessage());
      solution.abort();
      return;
    }

    //Run Qiskit solver via console
    try {
      Runtime rt = Runtime.getRuntime();
      Process exec = rt.exec("python maxCut_qiskit.py %s %s".formatted(problemPath, solutionPath), null, maxCutDirectory); //TODO: find location for python scripts

      if (exec.waitFor() == 0) {
        solution.complete();
        solution.setSolutionData(Files.readString(solutionFile));
      }

      solution.setDebugData("Qiskit didn't complete solving MaxCut successfully");
      solution.abort();
    } catch (IOException | InterruptedException e) {
      solution.setDebugData("Solving MaxCut problem via Qiskit resulted in exception: " + e.getMessage());
      solution.abort();
    }
  }
}
