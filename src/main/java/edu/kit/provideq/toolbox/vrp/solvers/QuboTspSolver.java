package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.process.BinaryProcessRunner;
import edu.kit.provideq.toolbox.process.ProcessResult;
import edu.kit.provideq.toolbox.process.ProcessRunner;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#SAT} solver using a GAMS implementation.
 */
@Component
public class QuboTspSolver extends VrpSolver {
  private final String scriptDir;
  private final ApplicationContext context;
  protected ResourceProvider resourceProvider;

  @Autowired
  public QuboTspSolver(
    @Value("${lkh.directory.vrp}") String scriptDir,
      ApplicationContext context) {
    this.scriptDir = scriptDir;
    this.context = context;
  }


  @Autowired
  public void setResourceProvider(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  @Override
  public String getName() {
    return "TSP to QUBO Solver";
  }


  @Override
  public List<SubRoutineDefinition> getSubRoutines() {
      return List.of(
          new SubRoutineDefinition(ProblemType.QUBO,
              "How should the QUBO be solved?")
      );
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    // TODO Check weither the problem is a TSP problem
    return problem.type() == ProblemType.VRP;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool) {
      
      // TODO change this to the correct script
  
      solution.setSolutionData("Not Implemented Yet!");
      solution.complete();
  }
}
