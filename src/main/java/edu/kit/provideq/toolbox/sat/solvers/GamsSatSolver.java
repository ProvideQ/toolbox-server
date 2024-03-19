package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#SAT} solver using a GAMS implementation.
 */
@Component
public class GamsSatSolver extends SatSolver {
  private final String satPath;
  private final ApplicationContext context;

  @Autowired
  public GamsSatSolver(
      @Value("${gams.directory.sat}") String satPath,
      ApplicationContext context) {
    this.satPath = satPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "GAMS SAT";
  }

  @Override
  public void solve(String input, Solution<DimacsCnfSolution> solution,
                    SubRoutinePool subRoutinePool) {
    DimacsCnf dimacsCnf;
    try {
      dimacsCnf = DimacsCnf.fromString(input);
      solution.setDebugData("Using cnf input: " + dimacsCnf);
    } catch (ConversionException | RuntimeException e) {
      solution.setDebugData("Parsing error: " + e.getMessage());
      solution.abort();
      return;
    }

    // Run SAT with GAMS via console
    var processResult = context
        .getBean(
            GamsProcessRunner.class,
            satPath,
            "sat.gms")
        .run(getProblemType(), solution.getId(), dimacsCnf.toString());

    if (processResult.success()) {
      var dimacsCnfSolution = DimacsCnfSolution.fromString(dimacsCnf, processResult.output());

      solution.setSolutionData(dimacsCnfSolution);
      solution.complete();
    } else {
      solution.setDebugData(processResult.output());
      solution.fail();
    }
  }
}
