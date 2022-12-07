package edu.kit.provideq.toolbox.maxCut;

import edu.kit.provideq.toolbox.maxCut.solvers.MaxCutSolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;

/**
 * Simple {@link MetaSolver} for MaxCut problems
 */
public class MetaSolverMaxCut extends MetaSolver<MaxCutSolver> {

  public MetaSolverMaxCut() {
    super();
    //TODO: register MaxCut Solvers
  }
}
