package edu.kit.provideq.toolbox.sat.convert;

import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.Solution;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GamsSATSolver implements ProblemSolver<String, String> {
    private final File gamsDirectory = new File(System.getProperty("user.dir"), "gams");
    private final File satDirectory = new File(gamsDirectory, "sat");
    private final File workingDirectory = new File(System.getProperty("user.dir"), "jobs");//todo move working directory to config


    @Override
    public boolean canSolve(Problem<String> problem) {
        //TODO: assess problemData
        return problem.type() == ProblemType.SAT;
    }

    @Override
    public float getSuitability(Problem<String> problem) {
        //TODO: implement algorithm for suitability calculation
        return 1;
    }

    @Override
    public void solve(Problem<String> problem, Solution<String> solution) {
        String dimacsCNF = BoolExprToDimacsCNF.Convert(problem.problemData());

        Path dir = Paths.get(workingDirectory.toString(), "sat", String.valueOf(solution.id()));
        Path problemFile = Paths.get(dir.toString(), "problem.cnf");
        Path solutionFile = Paths.get(dir.toString(), "problem.sol");

        //Write problem file
        try {
            Files.createDirectories(dir);
            Files.writeString(problemFile, dimacsCNF);
        } catch (IOException e) {
            solution.setDebugData("Creation of problem file caught exception: " + e.getMessage());
            solution.abort();
        }

        //Run SAT with GAMS via console
        try {
            Runtime rt = Runtime.getRuntime();
            Process exec = rt.exec("gams sat.gms --CNFINPUT=\"%s\"".formatted(problemFile), null, satDirectory);

            if (exec.waitFor() == 0) {
                solution.complete();
                solution.setSolutionData(Files.readString(solutionFile));
            }

            solution.setDebugData("GAMS didn't complete solving SAT successfully");
            solution.abort();
        } catch (IOException | InterruptedException e) {
            solution.setDebugData("Solving SAT problem via GAMS resulted in exception: " + e.getMessage());
            solution.abort();
        }
    }
}
