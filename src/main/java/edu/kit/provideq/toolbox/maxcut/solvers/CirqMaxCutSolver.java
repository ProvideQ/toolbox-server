package edu.kit.provideq.toolbox.maxcut.solvers;

import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#MAX_CUT} solver using a Cirq QAOA implementation.
 */
@Component
public class CirqMaxCutSolver extends MaxCutSolver {
  private final ApplicationContext context;
  private final String scriptDir;

  @Autowired
  public CirqMaxCutSolver(
      @Value("${cirq.directory.max-cut}") String scriptDir,
      ApplicationContext context) {
    this.scriptDir = scriptDir;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Cirq MaxCut";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.MAX_CUT;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {
    var processResult = context.getBean(
        PythonProcessRunner.class,
        scriptDir,
        "max_cut_cirq.py")
        .addProblemFilePathToProcessCommand()
        .addSolutionFilePathToProcessCommand()
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
