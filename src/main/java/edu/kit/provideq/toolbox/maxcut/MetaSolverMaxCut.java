package edu.kit.provideq.toolbox.maxcut;

import edu.kit.provideq.toolbox.maxcut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.MaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.QiskitMaxCutSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for MaxCut problems.
 */
@Component
public class MetaSolverMaxCut extends MetaSolver<MaxCutSolver> {

  @Autowired
  public MetaSolverMaxCut(QiskitMaxCutSolver qiskitMaxCutSolver,
                          GamsMaxCutSolver gamsMaxCutSolver) {
    super(qiskitMaxCutSolver, gamsMaxCutSolver);
  }

  @Override
  public MaxCutSolver findSolver(Problem problem, List<MetaSolverSetting> metaSolverSettings) {
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }
}
