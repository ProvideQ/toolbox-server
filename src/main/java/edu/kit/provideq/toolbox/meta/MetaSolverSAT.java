package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.sat.convert.SATSolver;

/**
 * Simple {@link MetaSolver} for SAT problems
 */
public class MetaSolverSAT extends MetaSolver<SATSolver> {

  public MetaSolverSAT() {
    super();
    this.registerSolver(new SATSolver());
    //TODO: register more SAT Solvers
  }
}
