package edu.kit.provideq.toolbox.maxCut;

import edu.kit.provideq.toolbox.maxCut.solvers.CirqMaxCutSolver;
import edu.kit.provideq.toolbox.maxCut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxCut.solvers.MaxCutSolver;
import edu.kit.provideq.toolbox.maxCut.solvers.QiskitMaxCutSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Random;

/**
 * Simple {@link MetaSolver} for MaxCut problems
 */
@Component
public class MetaSolverMaxCut extends MetaSolver<MaxCutSolver> {

  @Autowired
  public MetaSolverMaxCut(QiskitMaxCutSolver qiskitMaxCutSolver,
                          GamsMaxCutSolver gamsMaxCutSolver,
                          CirqMaxCutSolver cirqMaxCutSolver) {
    super(qiskitMaxCutSolver, gamsMaxCutSolver, cirqMaxCutSolver);
  }

  @Override
  public MaxCutSolver findSolver(Problem problem) {
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }
}
