package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * {@link ProblemType#QUBO} solver using a Dwaves Quantum Annealer implementation.
 */
@Component
public class DwaveQuboSolver extends QuboSolver {
  private final String quboPath;
  private final ApplicationContext context;

  @Autowired
  public DwaveQuboSolver(
      @Value("${qiskit.directory.qubo}") String quboPath,
      ApplicationContext context) {
    this.quboPath = quboPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Dwave QUBO Quantum Annealer";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.QUBO;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool) {
    // Run Dwave python script via console

    solution.setSolutionData("");
    solution.complete();
  }
}
