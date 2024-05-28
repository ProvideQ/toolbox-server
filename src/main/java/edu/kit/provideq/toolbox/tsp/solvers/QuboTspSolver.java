package edu.kit.provideq.toolbox.tsp.solvers;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.kit.provideq.toolbox.SolutionStatus.INVALID;

/**
 * Transforms TSP Problems into QUBOs
 */
@Component
public class QuboTspSolver extends TspSolver {
    private final ApplicationContext context;
    private final String binaryDir;
    private final String binaryName;
    private ResourceProvider resourceProvider;

    @Autowired
    public QuboTspSolver(
        @Value("${vrp.directory}") String binaryDir, //"vrp" value is correct because this uses the VRP framework from Lucas Bergers bachelor thesis
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
        return "TSP to QUBO Transformation";
    }

    private static final SubRoutineDefinition<String, String> QUBO_SUBROUTINE =
            new SubRoutineDefinition<>(QuboConfiguration.QUBO, "How should the QUBO be solved?");

    @Override
    public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
        return List.of(QUBO_SUBROUTINE);
    }

    public static boolean checkVehicleCapacity(String tsp) {
        int capacity = 0;
        int totalDemand = 0;
        boolean demandSection = false;

        Pattern capacityPattern = Pattern.compile("CAPACITY\\s*:\\s*(\\d+)");
        Pattern demandPattern = Pattern.compile("^\\d+\\s*(\\d+)");

        for ( String line : tsp.split("\n")) {
            Matcher capacityMatcher = capacityPattern.matcher(line);
            if (capacityMatcher.find()) {
                capacity = Integer.parseInt(capacityMatcher.group(1));
            }
            if (line.startsWith("DEMAND_SECTION")) {
                demandSection = true;
                continue;
            }
            if (line.startsWith("EOF")) {
                demandSection = false;
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
    public Mono<Solution<String>> solve(
        String input,
        SubRoutineResolver resolver
    ) {
        return resolver.runSubRoutine(QUBO_SUBROUTINE, input)
                .map(quboSolution -> {
                    //TODO: refactor old code and retrieve qubo solution here
                   return new Solution<String>();
                });

        /* old code:
        var solution = new Solution<String>();

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
            .run(getProblemType(), solution.getId(), input);
        
        if (!processResult.success() || processResult.output().isEmpty()) {
            solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
            solution.abort();
            return Mono.just(solution);
        }

        // solve qubo with sub-routine
        var quboSolver = subRoutinePool.<String, String>getSubRoutine(ProblemType.QUBO);
        var quboSolution = quboSolver.apply(processResult.output().get());
        if (quboSolution.getStatus() == INVALID) {
            solution.setDebugData(quboSolution.getDebugData());
            solution.abort();
            return Mono.just(solution);
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
            return Mono.just(solution);
        }

        var quboSolutionFilePath = Path.of(problemDirectoryPath, "problem.bin");

        try {
            Files.writeString(quboSolutionFilePath, quboSolution.getSolutionData());
        } catch (IOException e) {
            solution.setDebugData("Failed to write qubo solution file with path: " + quboSolutionFilePath.toString());
            solution.abort();
            return Mono.just(solution);
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
        .run(getProblemType(), solution.getId(), input);
    
        if (!processRetransformResult.success()) {
            solution.setDebugData(processRetransformResult.errorOutput().orElse("Unknown error occurred."));
            solution.abort();
            return Mono.just(solution);
        }

        solution.setSolutionData(processRetransformResult.output().orElse("Empty Solution"));
        solution.complete();
        return Mono.just(solution);*/
    }
}
