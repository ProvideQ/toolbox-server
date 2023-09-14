package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

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
  public boolean canSolve(Problem<String> problem) {
    //TODO: assess problemData
    return problem.type() == ProblemType.SAT;
  }

  @Override
  public void solve(Problem<String> problem, Solution<DimacsCnfSolution> solution,
                    SubRoutinePool subRoutinePool) {
    DimacsCnf dimacsCnf;
    try {
      dimacsCnf = DimacsCnf.fromString(problem.problemData());
      solution.setDebugData("Using cnf input: " + dimacsCnf);
    } catch (ConversionException | RuntimeException e) {
      solution.setDebugData("Parsing error: " + e.getMessage());
      return;
    }

    // Run SAT with GAMS via console
    var processResult = context
        .getBean(
            GamsProcessRunner.class,
            satPath,
            "sat.gms")
        .run(problem.type(), solution.getId(), dimacsCnf.toString());

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
