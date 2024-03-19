package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.PythonProcessRunner;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.qubo.QuboProblemManagerConfiguration;
import edu.kit.provideq.toolbox.test.SubRoutineResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link QuboProblemManagerConfiguration#QUBO} solver using a Qiskit QAOA implementation.
 */
@Component
public class QiskitQuboSolver extends QuboSolver {
  private final String quboPath;
  private final ApplicationContext context;

  @Autowired
  public QiskitQuboSolver(
      @Value("${qiskit.directory.qubo}") String quboPath,
      ApplicationContext context) {
    this.quboPath = quboPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Qiskit QUBO";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == QuboProblemManagerConfiguration.QUBO;
  }

  @Override
  public Mono<Solution<String>> solve(Problem<String> problem,
                                      SubRoutineResolver subRoutineResolver) {
    var solution = new Solution<String>();

    // Run Qiskit solver via console
    var processResult = context
        .getBean(
            PythonProcessRunner.class,
            quboPath,
            "qubo_qiskit.py")
        .addProblemFilePathToProcessCommand()
        .addSolutionFilePathToProcessCommand()
        .problemFileName("problem.lp")
        .run(
            problem.type(),
            (long) (Math.random() * Long.MAX_VALUE), // TODO
            problem.problemData()
        );

    // Return if process failed
    if (!processResult.success()) {
      solution.setDebugData(processResult.output());
      solution.abort();
    } else {
      solution.setSolutionData(processResult.output());
      solution.complete();
    }
    return Mono.just(solution);
  }
}
