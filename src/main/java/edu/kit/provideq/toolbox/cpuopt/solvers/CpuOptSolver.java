package edu.kit.provideq.toolbox.cpuopt.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.cpuopt.CpuOptConfiguration;
import edu.kit.provideq.toolbox.cpuopt.solvers.nn.MLP;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

@Component
public class CpuOptSolver implements ProblemSolver<String, String> {

    @Override
    public String getName() {
        return CpuOptConfiguration.CPUOPT.getId();
    }

    @Override
    public String getDescription() {
        return CpuOptConfiguration.CPUOPT.getDescription();
    }

    @Override
    public Mono<Solution<String>> solve(String input, SubRoutineResolver subRoutineResolver,
                                        SolvingProperties properties) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            MLP mlp = mapper.readValue(
                    new File("mlp.json"),
                    MLP.class
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public ProblemType<String, String> getProblemType(){
        return CpuOptConfiguration.CPUOPT;
    }
}
