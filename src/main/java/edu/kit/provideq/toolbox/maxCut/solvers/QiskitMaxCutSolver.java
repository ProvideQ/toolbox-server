package edu.kit.provideq.toolbox.maxCut.solvers;

import edu.kit.provideq.toolbox.QiskitProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class QiskitMaxCutSolver extends MaxCutSolver{
  private final String maxCutPath;
  private final ApplicationContext context;

  @Autowired
  public QiskitMaxCutSolver(
          @Value("${qiskit.directory.max-cut}") String maxCutPath,
          ApplicationContext context) {
    this.maxCutPath = maxCutPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Qiskit MaxCut";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    //TODO: assess problemData
    return problem.type() == ProblemType.MAX_CUT;
  }

  @Override
  public float getSuitability(Problem<String> problem) {
    //TODO: implement algorithm for suitability calculation
    return 1;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution, SubRoutinePool subRoutinePool) {
    // Run Qiskit solver via console
    var processResult = context
            .getBean(
                    QiskitProcessRunner.class,
                    maxCutPath,
                    "maxCut_qiskit.py")
            .addProblemFilePathToProcessCommand()
            .addSolutionFilePathToProcessCommand()
            .run(problem.type(), solution.getId(), problem.problemData());

    if (processResult.success()) {
      solution.setSolutionData(processResult.output());
      solution.complete();
    } else {
      solution.setDebugData(processResult.output());
      solution.abort();
    }
  }
}
