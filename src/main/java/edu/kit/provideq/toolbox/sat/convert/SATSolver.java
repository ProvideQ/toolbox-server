package edu.kit.provideq.toolbox.sat.convert;

import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.SolutionResult;
import edu.kit.provideq.toolbox.SolutionStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SATSolver {
    private final File gamsDirectory = new File(System.getProperty("user.dir"), "gams");
    private final File satDirectory = new File(gamsDirectory, "sat");

    private final File workingDirectory = new File(System.getProperty("user.dir"), "jobs");//todo move working directory to config

    public SolutionResult Solve(String expression, SolutionHandle handle) {
        String dimacsCNF = BoolExprToDimacsCNF.Convert(expression);

        Path dir = Paths.get(workingDirectory.toString(), "sat", String.valueOf(handle.id()));
        Path problemFile = Paths.get(dir.toString(), "problem.cnf");
        Path solutionFile = Paths.get(dir.toString(), "problem.sol");

        //Write problem file
        try {
            Files.createDirectories(dir);
            Files.writeString(problemFile, dimacsCNF);
        } catch (IOException e) {
            handle.setStatus(SolutionStatus.INVALID);
            return new SolutionResult("", "", "Creation of problem file caught exception: " + e.getMessage());
        }

        //Run SAT with GAMS via console
        try {
            Runtime rt = Runtime.getRuntime();
            Process exec = rt.exec("gams sat.gms --CNFINPUT=\"%s\"".formatted(problemFile), null, satDirectory);

            if (exec.waitFor() == 0) {
                handle.setStatus(SolutionStatus.SOLVED);
                String solutionText = Files.readString(solutionFile);
                return new SolutionResult(solutionText, "", "");
            }

            handle.setStatus(SolutionStatus.INVALID);
            return new SolutionResult("", "", "GAMS didn't complete solving SAT successfully");
        } catch (IOException | InterruptedException e) {
            handle.setStatus(SolutionStatus.INVALID);
            return new SolutionResult("", "", "Solving SAT problem via GAMS resulted in exception: " + e.getMessage());
        }
    }
}
