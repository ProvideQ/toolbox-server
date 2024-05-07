package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.process.BinaryProcessRunner;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#QUBO} solver using a Qrisps QAOA implementation.
 */
@Component
public class QrispQuboSolver extends QuboSolver {
  private final String vrpPath;
  private final ApplicationContext context;

  @Autowired
  public QrispQuboSolver(
      @Value("${qrisp.directory.vrp}") String vrpPath,
      ApplicationContext context) {
    this.vrpPath = vrpPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Qrisp QAOA VRP QUBO Solver";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.QUBO;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {
    
    var processRunner = context.getBean(
      //TODO: Change to PythonProcessRunner, Install Binaries (for other Solvers) on Server
        BinaryProcessRunner.class,
        vrpPath,
        "/Users/koalamitice/opt/anaconda3/bin/python",
        "qaoa.py",
        new String[] {"%1$s", "--output-file", "%2$s", "--size-gate", "4"}
        )
        .problemFileName("problem.lp")
        .solutionFileName("problem.bin");

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
