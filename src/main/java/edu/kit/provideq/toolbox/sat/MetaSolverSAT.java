package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.sat.solvers.GamsSATSolver;
import edu.kit.provideq.toolbox.sat.solvers.SATSolver;

/**
 * Simple {@link MetaSolver} for SAT problems
 */
public class MetaSolverSAT extends MetaSolver<SATSolver> {

  public MetaSolverSAT() {
    super();
    this.registerSolver(new GamsSATSolver());
    //TODO: register more SAT Solvers
  }
}
