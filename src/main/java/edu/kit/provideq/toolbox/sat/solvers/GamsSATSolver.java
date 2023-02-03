package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.ProcessRunner;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.Solution;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.sat.convert.BoolExprToDimacsCNF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class GamsSATSolver extends SATSolver {
    private final File satDirectory;

    private final ResourceProvider resourceProvider;

    @Autowired
    public GamsSATSolver(
            @Value("${gams.directory.sat}") String satPath,
            ResourceProvider resourceProvider) throws IOException {
        this.resourceProvider = resourceProvider;

        satDirectory = resourceProvider.getResource(satPath);
    }

    @Override
    public String getName() {
        return "GAMS SAT";
    }

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
        String dimacsCNF;
        try {
            dimacsCNF = BoolExprToDimacsCNF.convert(problem.problemData());
            solution.setDebugData("Using cnf input: " + dimacsCNF);
        } catch (RuntimeException e) {
            solution.setDebugData("Parsing error: " + e.getMessage());
            return;
        }

        Path problemFile;
        Path solutionFile;

        // Write problem file
        try {
            File problemDirectory = resourceProvider.getProblemDirectory(problem, solution);

            problemFile = Paths.get(problemDirectory.getAbsolutePath(), "problem.cnf");
            solutionFile = Paths.get(problemDirectory.getAbsolutePath(), "problem.sol");

            Files.writeString(problemFile, dimacsCNF);
        } catch (IOException e) {
            solution.setDebugData("Creation of problem file caught exception: " + e.getMessage());
            solution.abort();
            return;
        }

        //Run SAT with GAMS via console
        try {
            Runtime rt = Runtime.getRuntime();
            Process exec = rt.exec("gams sat.gms --CNFINPUT=\"%s\"".formatted(problemFile), null, satDirectory);

            if (exec.waitFor() == 0) {
                solution.complete();
                solution.setSolutionData(Files.readString(solutionFile));
                return;
            }

            solution.setDebugData("GAMS didn't complete solving SAT successfully");
            solution.abort();
        } catch (IOException | InterruptedException e) {
            solution.setDebugData("Solving SAT problem via GAMS resulted in exception: " + e.getMessage());
            solution.abort();
        }
    }
}
