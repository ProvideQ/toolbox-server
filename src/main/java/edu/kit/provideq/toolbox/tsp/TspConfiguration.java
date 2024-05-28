package edu.kit.provideq.toolbox.tsp;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.tsp.solvers.QuboTspSolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@Configuration
public class TspConfiguration {

    /**
     * A Traveling Sales Person Problem
     * Optimization Problem with the goal of find an optimal route between a given set of connected cities
     */
    public static final ProblemType<String, String> TSP = new ProblemType<>(
            "tsp",
            String.class,
            String.class
    );

    @Bean
    ProblemManager<String, String> getTspManager(
            ResourceProvider provider,
            QuboTspSolver quboTspSolver
    ) {
        return new ProblemManager<>(TSP,
                Set.of(quboTspSolver),
                loadExampleProblems(provider));
    }

    private Set<Problem<String, String>> loadExampleProblems(ResourceProvider provider) {
        try {
            var problemSteam = Objects.requireNonNull(getClass().getResourceAsStream("att48.tsp"));
            var problem = new Problem<>(TSP);
            problem.setInput(provider.readStream(problemSteam));
            return Set.of(problem);
        } catch (IOException e) {
            throw new RuntimeException("Could not load example problems", e);
        }
    }
}
