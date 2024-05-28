package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.vrp.VrpConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link VrpConfiguration#VRP} solver using Qrisp QAOA implementation
 */
@Component
public class QrispVrpSolver extends VrpSolver {
  private final String scriptDir;
  private final ApplicationContext context;

  @Autowired
  public QrispVrpSolver(
    @Value("${qrisp.directory.vrp}") String scriptDir,
      ApplicationContext context) {
    this.scriptDir = scriptDir;
    this.context = context;
  }

  @Override
  public String getName() {
    return "Grover-based VRP Solver (Qrisp)";
  }

  @Override
  public Mono<Solution<String>> solve(
          String input,
          SubRoutineResolver resolver
  ) {

    var solution = new Solution<String>();

    var processResult = context.getBean(
                    PythonProcessRunner.class,
                    scriptDir,
                    "grover.py",
                    new String[]{"--size-gate", "35"}
            )
            .addProblemFilePathToProcessCommand()
            .addSolutionFilePathToProcessCommand("--output-file", "%s")
            .problemFileName("problem.vrp")
            .solutionFileName("problem.sol")
            .run(getProblemType(), solution.getId(), input);

    return Mono.just(processResult.applyTo(solution));
  }
}
