package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.setting.*;
import edu.kit.provideq.toolbox.sat.solvers.GamsSatSolver;
import edu.kit.provideq.toolbox.sat.solvers.SatSolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple {@link MetaSolver} for SAT problems
 */
@Component
public class MetaSolverSat extends MetaSolver<SatSolver> {

  @Autowired
  public MetaSolverSat(GamsSatSolver gamsSatSolver) {
    super(gamsSatSolver);
    //TODO: register more SAT Solvers
  }

  @Override
  public SatSolver findSolver(Problem problem, List<MetaSolverSetting> metaSolverSettings) {
    // todo add decision
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }
}
