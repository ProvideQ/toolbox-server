package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;

import static edu.kit.provideq.toolbox.SolutionStatus.INVALID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#SAT} solver using a GAMS implementation.
 */
@Component
public class QuboTspSolver extends VrpSolver {
    private final ApplicationContext context;
    private final String binaryDir;
    private final String binaryName;
    private ResourceProvider resourceProvider;

    @Autowired
    public QuboTspSolver(
        @Value("${vrp.directory}") String binaryDir,
        @Value("${vrp.bin.meta-solver}") String binaryName,
        ApplicationContext context) {
        this.binaryName = binaryName;
        this.binaryDir = binaryDir;
        this.context = context;
    }


    @Autowired
    public void setResourceProvider(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    @Override
    public String getName() {
        return "TSP to QUBO Solver (Classical)";
    }


    @Override
    public List<SubRoutineDefinition> getSubRoutines() {
        return List.of(
            new SubRoutineDefinition(ProblemType.QUBO,
                "How should the QUBO be solved?")
        );
    }

    @Override
    public boolean canSolve(Problem<String> problem) {
        boolean isTSP = checkVehicleCapacity(problem.problemData());
        return problem.type() == ProblemType.VRP && isTSP;
    }

    public static boolean checkVehicleCapacity(String vrp) {
            int capacity = 0;
            int totalDemand = 0;
            boolean demandSection = false;

            Pattern capacityPattern = Pattern.compile("CAPACITY\\s*:\\s*(\\d+)");
            Pattern demandPattern = Pattern.compile("\\d+\\s*(\\d+)");

            for ( String line : vrp.split("\n")) {
                Matcher capacityMatcher = capacityPattern.matcher(line);
                if (capacityMatcher.find()) {
                    capacity = Integer.parseInt(capacityMatcher.group(1));
                }
                if (line.startsWith("DEMAND_SECTION")) {
                    demandSection = true;
                    continue;
                }
                if (line.startsWith("EOF")) {
                    break;
                }
                if (demandSection) {
                    Matcher demandMatcher = demandPattern.matcher(line);
                    if (demandMatcher.find()) {
                        totalDemand += Integer.parseInt(demandMatcher.group(1));
                    }
                }
            }

            return totalDemand <= capacity;
        }

    @Override
    public void solve(Problem<String> problem, Solution<String> solution,
                        SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {

        // translate into qubo in lp-file format with rust vrp meta solver
        var processResult = context.getBean(
                BinaryProcessRunner.class,
                binaryDir,
                binaryName,
                "partial",
                new String[] { "solve", "%1$s", "simulated", "--transform-only"}
            )
            .problemFileName("problem.vrp")
            .solutionFileName("problem.lp")
            .run(problem.type(), solution.getId(), problem.problemData());
        
        if (!processResult.success()) {
            solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
            solution.abort();
            return;
        }

        // solve qubo with sub-routine
        var quboSolver = subRoutinePool.<String, String>getSubRoutine(ProblemType.QUBO);
        var quboSolution = quboSolver.apply(processResult.output().get());
        if (quboSolution.getStatus() == INVALID) {
            solution.setDebugData(quboSolution.getDebugData());
            solution.abort();
            return;
        }

        // write solution to current problem directory
        String problemDirectoryPath;
        try {
            problemDirectoryPath = resourceProvider
                .getProblemDirectory(problem.type(), solution.getId())
                .getAbsolutePath();
        } catch (IOException e) {
            solution.setDebugData("Failed to retrieve problem directory.");
            solution.abort();
            return;
        }

        var quboSolutionFilePath = Path.of(problemDirectoryPath, "problem.bin");

        try {
            Files.writeString(quboSolutionFilePath, quboSolution.getSolutionData());
        } catch (IOException e) {
            solution.setDebugData("Failed to write qubo solution file with path: " + quboSolutionFilePath.toString());
            solution.abort();
            return;
        }
        
        var processRetransformResult = context.getBean(
            BinaryProcessRunner.class,
            binaryDir,
            binaryName,
            "partial",
            new String[] { "solve", "%1$s", "simulated", "--qubo-solution", quboSolutionFilePath.toString()}
        )
        .problemFileName("problem.vrp")
        .solutionFileName("problem.sol")
        .run(problem.type(), solution.getId(), problem.problemData());
    
        if (!processRetransformResult.success()) {
            solution.setDebugData(processRetransformResult.errorOutput().orElse("Unknown error occurred."));
            solution.abort();
            return;
        }

        solution.setSolutionData(processRetransformResult.output().orElse("Empty Solution"));
        solution.complete();
    }
}
