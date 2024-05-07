package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
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
 * {@link ProblemType#SAT} solver using a GAMS implementation.
 */
@Component
public class LkhVrpSolver extends VrpSolver {
  private final String scriptDir;
  private final ApplicationContext context;

  @Autowired
  public LkhVrpSolver(
    @Value("${lkh.directory.vrp}") String scriptDir,
      ApplicationContext context) {
    this.scriptDir = scriptDir;
    this.context = context;
  }

  @Override
  public String getName() {
    return "LKH-3 VRP Solver (Classical)";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.VRP;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {
      
      var processResult = context.getBean(
          BinaryProcessRunner.class,
          scriptDir,
          "/Users/koalamitice/opt/anaconda3/bin/python",
          "vrp_lkh.py"
        )
        .addProblemFilePathToProcessCommand()
        .addSolutionFilePathToProcessCommand("--output-file", "%s")
        .problemFileName("problem.vrp")
        .solutionFileName("problem.sol")
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
