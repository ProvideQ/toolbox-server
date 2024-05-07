package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.meta.setting.Select;
import edu.kit.provideq.toolbox.meta.setting.Text;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#QUBO} solver using a Dwaves Quantum Annealer implementation.
 */
@Component
public class DwaveQuboSolver extends QuboSolver {
    private final String quboScriptPath;
    private final ApplicationContext context;

    private static final String METHOD_SETTING_NAME = "method";
    private static final String API_TOKEN_SETTING_NAME = "dwave-token";

    @Autowired
    public DwaveQuboSolver(
        @Value("${dwave.directory.qubo}") String quboScriptPath,
        ApplicationContext context) {
        this.quboScriptPath = quboScriptPath;
        this.context = context;
    }

    @Override
    public String getName() {
        return "Dwave QUBO Quantum Annealer";
    }

    @Override
    public boolean canSolve(Problem<String> problem) {
        return problem.type() == ProblemType.QUBO;
    }

    @Override
    public List<MetaSolverSetting> getSettings() {
        return List.of(
            new Select<String>(METHOD_SETTING_NAME, "DWave Annealing Method", List.of("sim", "hybrid", "qbsolv", "direct"), "sim"),
            new Text(API_TOKEN_SETTING_NAME, "DWave API Token (required for non-sim methods)")
        );
    }

    @Override
    public void solve(Problem<String> problem, Solution<String> solution,
                        SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {
        
        @SuppressWarnings("unchecked")
        String dwaveAnnealingMethod = settings.stream()
            .filter(setting -> setting.name.equals(METHOD_SETTING_NAME))
            .map(setting -> ((Select<String>) setting))
            .findFirst()
            .map(setting -> setting.selectedOption)
            .orElse("sim");

        Optional<String> dwaveToken = settings.stream()
            .filter(setting -> setting.name.equals(API_TOKEN_SETTING_NAME))
            .map(setting -> ((Text) setting))
            .findFirst()
            .map(setting -> setting.text);
            

        var processRunner = context.getBean(
            BinaryProcessRunner.class,
            quboScriptPath,
            "/Users/koalamitice/opt/anaconda3/bin/python",
            "main.py",
            new String[] {"%1$s", dwaveAnnealingMethod, "--output-file", "%2$s"}
            )
            .problemFileName("problem.lp")
            .solutionFileName("problem.bin");

        if (dwaveToken.isPresent()) {
            processRunner.addEnvironmentVariable("DWAVE_API_TOKEN", dwaveToken.get());
        }

        var processResult = processRunner
            .run(problem.type(), solution.getId(), problem.problemData());
            
        if (!processResult.success()) {
            solution.setDebugData(processResult.errorOutput().orElse("Unknown error occurred."));
            solution.abort();
            return;
        }

        solution.setSolutionData(processResult.output().orElse("Empty Solution"));
        solution.complete();
    }
}
