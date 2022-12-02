package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.sat.convert.GamsSATSolver;

/**
 * Simple {@link MetaSolver} for SAT problems
 */
public class MetaSolverSAT extends MetaSolver<GamsSATSolver> {

  public MetaSolverSAT() {
    super();
    this.registerSolver(new GamsSATSolver());
    //TODO: register more SAT Solvers
  }
}
