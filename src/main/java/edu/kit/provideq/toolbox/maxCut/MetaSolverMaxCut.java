package edu.kit.provideq.toolbox.maxCut;

import edu.kit.provideq.toolbox.maxCut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxCut.solvers.MaxCutSolver;
import edu.kit.provideq.toolbox.maxCut.solvers.QiskitMaxCutSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import java.util.ArrayList;
import java.util.Random;

/**
 * Simple {@link MetaSolver} for MaxCut problems
 */
public class MetaSolverMaxCut extends MetaSolver<MaxCutSolver> {

  public MetaSolverMaxCut() {
    super(new QiskitMaxCutSolver(), new GamsMaxCutSolver());
  }

  @Override
  public MaxCutSolver findSolver(Problem problem) {
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }
}
