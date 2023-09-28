package edu.kit.provideq.toolbox.qubo;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.qubo.solvers.QiskitQuboSolver;
import edu.kit.provideq.toolbox.qubo.solvers.QuboSolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for MaxCut problems.
 */
@Component
public class QuboMetaSolver extends MetaSolver<String, String, QuboSolver> {

  @Autowired
  public QuboMetaSolver(QiskitQuboSolver qiskitQuboSolver) {
    super(ProblemType.QUBO, qiskitQuboSolver);
  }

  @Override
  public QuboSolver findSolver(
          Problem<String> problem,
          List<MetaSolverSetting> metaSolverSettings) {
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }

  @Override
  public List<String> getExampleProblems() {
    return List.of("""
        Maximize
          3x + y
        Subject To
        Binary
          x y
        End
        """);
  }
}
