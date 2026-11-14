package edu.kit.provideq.toolbox.cpuopt;



import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.exception.MissingExampleException;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.cpuopt.solvers.CpuOptSolver;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class CpuOptConfiguration {

    public static final ProblemType<String, String> CPUOPT = new ProblemType<>(
            "cpuopt",
            "An optimization problem: For a given CPU select for the next time slice the best "
            + "configuration (CPU thread + frequency) which optimizes energy and/or performance.",
            String.class,
            String.class
    );
    @Bean
    ProblemManager<String,String> getCPUOptManager(

            CpuOptSolver cpuOptSolver,
            ResourceProvider resourceProvider

    ){

        return new ProblemManager<>(
                CPUOPT,
                Set.of(cpuOptSolver),
                loadExampleProblems(resourceProvider)
                );
    }

    private Set<Problem<String, String>> loadExampleProblems(
            ResourceProvider resourceProvider
    ) {
        try {
            var problemInputStream = Objects.requireNonNull(
                    getClass().getResourceAsStream("cpu_example.txt"),
                    "example for cpu opt is unavailable!"
            );
            var problem = new Problem<>(CPUOPT);
            problem.setInput(resourceProvider.readStream(problemInputStream));
            return Set.of(problem);
        } catch (IOException e) {
            throw new MissingExampleException(CPUOPT, e);
        }
    }

}
