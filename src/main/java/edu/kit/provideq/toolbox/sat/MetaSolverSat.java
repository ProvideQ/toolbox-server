package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import edu.kit.provideq.toolbox.sat.solvers.GamsSatSolver;
import edu.kit.provideq.toolbox.sat.solvers.SatSolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for SAT problems.
 */
@Component
public class MetaSolverSat extends MetaSolver<String, DimacsCnfSolution, SatSolver> {

  @Autowired
  public MetaSolverSat(GamsSatSolver gamsSatSolver) {
    super(ProblemType.SAT, gamsSatSolver);
    //TODO: register more SAT Solvers
  }

  @Override
  public SatSolver findSolver(Problem<String> problem, List<MetaSolverSetting> metaSolverSettings) {
    // todo add decision
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }

  @Override
  public List<String> getExampleProblems() {
    return List.of(
        "a and b",
        "a and b or c"
    );
  }
}
