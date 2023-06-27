package edu.kit.provideq.toolbox.sat.solvers;

import edu.kit.provideq.toolbox.GamsProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCNF;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCNFSolution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class GamsSATSolver extends SATSolver {
  private final String satPath;
  private final ApplicationContext context;

  @Autowired
  public GamsSATSolver(
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
  public float getSuitability(Problem<String> problem) {
    //TODO: implement algorithm for suitability calculation
    return 1;
  }

  @Override
  public void solve(Problem<String> problem, Solution<DimacsCNFSolution> solution,
                    SubRoutinePool subRoutinePool) {
    DimacsCNF dimacsCNF;
    try {
      dimacsCNF = DimacsCNF.fromString(problem.problemData());
      solution.setDebugData("Using cnf input: " + dimacsCNF);
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
        .run(problem.type(), solution.getId(), dimacsCNF.toString());

    if (processResult.success()) {
      var dimacsCNFSolution = DimacsCNFSolution.fromString(dimacsCNF, processResult.output());

      solution.setSolutionData(dimacsCNFSolution);
      solution.complete();
    } else {
      solution.setDebugData(processResult.output());
      solution.abort();
    }
  }
}
