package edu.kit.provideq.toolbox.maxCut.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.ResourceProvider;
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
public class GamsMaxCutSolver extends MaxCutSolver {
    private final File maxCutDirectory;

    private final ResourceProvider resourceProvider;

    @Autowired
    public GamsMaxCutSolver(
            @Value("${gams.directory.max-cut}") String maxCutPath,
            ResourceProvider resourceProvider) throws IOException {
        this.resourceProvider = resourceProvider;

        maxCutDirectory = resourceProvider.getResource(maxCutPath);
    }

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
        Path problemFile;
        Path solutionFile;

        // Write problem file
        try {
            File problemDirectory = resourceProvider.getProblemDirectory(problem, solution);

            problemFile = Paths.get(problemDirectory.getAbsolutePath(), "problem.gml");
            solutionFile = Paths.get(problemDirectory.getAbsolutePath(), "problem_sol.gml");

            Files.writeString(problemFile, problem.problemData());
        } catch (IOException e) {
            solution.setDebugData("Creation of problem file caught exception: " + e.getMessage());
            solution.abort();
            return;
        }

        // Run MaxCut with GAMS via console
        try {
            var processResult = new GamsProcessRunner(
                maxCutDirectory,
                "maxcut.gms",
                "--INPUT=\"%s\"".formatted(problemFile.toAbsolutePath().toString().replace('\\', '/'))
            ).run();

            if (processResult.success()) {
                solution.complete();
                solution.setSolutionData(Files.readString(solutionFile));
                return;
            }

            solution.setDebugData("GAMS didn't complete solving MaxCut successfully!\n" +
                processResult.output());
            solution.abort();
        } catch (IOException | InterruptedException e) {
            solution.setDebugData("Solving MaxCut problem via GAMS resulted in exception: " + e.getMessage());
            solution.abort();
        }
    }
}
