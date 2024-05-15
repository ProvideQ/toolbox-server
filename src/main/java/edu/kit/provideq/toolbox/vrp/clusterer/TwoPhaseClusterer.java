package edu.kit.provideq.toolbox.vrp.clusterer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.process.MultiFileProcessResultReader;
import reactor.core.publisher.Mono;

@Component
public class TwoPhaseClusterer extends VrpClusterer {
    
    @Autowired
    public TwoPhaseClusterer(
        @Value("${vrp.directory}") String binaryDir,
        @Value("${vrp.bin.meta-solver}") String binaryName,
        ApplicationContext context) {
            super(binaryDir, binaryName, context);
    }


    @Override
    public String getName() {
        return "Two Phase VRP To TSP Clusterer (Classical)";
    }

    @Override
    public Mono<Solution<String>> solve(
            String input,
            SubRoutineResolver resolver
    ) {

        var solution = new Solution<HashMap<Path, String>>();

        // cluster with tsp/two-phase clustering
        var processResult = context.getBean(
                        BinaryProcessRunner.class,
                        binaryDir,
                        binaryName,
                        "partial",
                        new String[]{"cluster", "%1$s", "tsp", "--build-dir", "%3$s/.vrp"}
                )
                .problemFileName("problem.vrp")
                .run(getProblemType(), solution.getId(), input, new MultiFileProcessResultReader("./.vrp/problem_*.vrp"));

        processResult.applyTo(solution);

        // solve the clusters
        //solveClusters();

        //TODO: return something useful
        return Mono.just(new Solution<String>());
    }

}
