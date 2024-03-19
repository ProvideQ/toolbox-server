package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
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
 * {@link ProblemType#QUBO} solver using a Dwaves Quantum Annealer implementation.
 */
@Component
public class DwaveQuboSolver extends QuboSolver {
  private final String quboScriptPath;
  private final ApplicationContext context;

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
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool, List<MetaSolverSetting> settings) {
    
      var processResult = context.getBean(
          BinaryProcessRunner.class,
          quboScriptPath,
          "../venv/bin/python",
          "main.py",
          new String[] {"%1$s", "sim", "--output-file", "%2$s"}
        )
        .problemFileName("problem.lp")
        .solutionFileName("problem.bin")
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
