package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCNF;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCNFSolution;
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
    public void solve(Problem<String> problem, Solution<DimacsCNFSolution> solution, SubRoutinePool subRoutinePool) {
        DimacsCNF dimacsCNF;
        try {
            dimacsCNF = DimacsCNF.fromString(problem.problemData());
            solution.setDebugData("Using cnf input: " + dimacsCNF);
        } catch (ConversionException | RuntimeException e) {
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

            Files.writeString(problemFile, dimacsCNF.toString());
        } catch (IOException e) {
            solution.setDebugData("Creation of problem file caught exception: " + e.getMessage());
            solution.abort();
            return;
        }

        // Run SAT with GAMS via console
        try {
            var processResult = new GamsProcessRunner(
                satDirectory,
                "sat.gms", "--CNFINPUT=\"%s\"".formatted(problemFile)
            ).run();

            if (processResult.success()) {
                var solutionText = Files.readString(solutionFile);
                var dimacsCNFSolution = DimacsCNFSolution.fromString(dimacsCNF, solutionText);

                solution.complete();
                solution.setSolutionData(dimacsCNFSolution);
                return;
            }

            solution.setDebugData("GAMS didn't complete solving SAT successfully!\n" +
                    processResult.output());
            solution.abort();
        } catch (IOException | InterruptedException e) {
            solution.setDebugData("Solving SAT problem via GAMS resulted in exception: " + e.getMessage());
            solution.abort();
        }
    }
}
