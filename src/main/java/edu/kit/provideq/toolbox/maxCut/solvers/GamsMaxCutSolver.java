package edu.kit.provideq.toolbox.maxCut.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GamsMaxCutSolver extends MaxCutSolver {
    private final File gamsDirectory = new File(System.getProperty("user.dir"), "gams");
    private final File maxCutDirectory = new File(gamsDirectory, "maxcut");
    private final File workingDirectory = new File(System.getProperty("user.dir"), "jobs");//todo move working directory to config

    @Override
    public String getName() {
        return "GAMS MaxCut";
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
        Path dir = Paths.get(workingDirectory.toString(), "maxcut", String.valueOf(solution.id()));
        Path problemFile = Paths.get(dir.toString(), "problem.gml");
        Path solutionFile = Paths.get(dir.toString(), "problem_sol.gml");

        //Write problem file
        try {
            Files.createDirectories(dir);
            Files.writeString(problemFile, problem.problemData());
        } catch (IOException e) {
            solution.setDebugData("Creation of problem file caught exception: " + e.getMessage());
            solution.abort();
            return;
        }

        //Run MaxCut with GAMS via console
        try {
            Runtime rt = Runtime.getRuntime();
            // problem file path can't use '\' characters, and no '/'
            Process exec = rt.exec("gams maxcut.gms --INPUT=\"%s\"".formatted(problemFile).replace('\\', '/'), null, maxCutDirectory);

            // Inputs needs to be consumed, otherwise the process won't progress
            var input = exec.inputReader();
            while (input.readLine() != null) {}
            input.close();

            int i = exec.waitFor();
            if (i == 0) {
                solution.complete();
                solution.setSolutionData(Files.readString(solutionFile));
                return;
            }

            solution.setDebugData("GAMS didn't complete solving MaxCut successfully");
            solution.abort();
        } catch (IOException | InterruptedException e) {
            solution.setDebugData("Solving MaxCut problem via GAMS resulted in exception: " + e.getMessage());
            solution.abort();
        }
    }
}
