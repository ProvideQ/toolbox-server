package edu.kit.provideq.toolbox.qubo.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link QuboConfiguration#QUBO} solver using a Qiskit QAOA implementation.
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
    return "(Qiskit) QAOA Solver for QUBOs";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver
  ) {
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
        .run(getProblemType(), solution.getId(), input);

    // Return if process failed
    return Mono.just(processResult.applyTo(solution));
  }
}
