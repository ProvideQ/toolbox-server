package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.sat.solvers.GamsSATSolver;
import edu.kit.provideq.toolbox.sat.solvers.SATSolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for SAT problems
 */
@Component
public class MetaSolverSAT extends MetaSolver<SATSolver> {

  @Autowired
  public MetaSolverSAT(GamsSATSolver gamsSATSolver) {
    super(gamsSATSolver);
    //TODO: register more SAT Solvers
  }
}
