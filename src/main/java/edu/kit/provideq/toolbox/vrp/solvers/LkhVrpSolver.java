package edu.kit.provideq.toolbox.vrp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import edu.kit.provideq.toolbox.vrp.VrpConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link VrpConfiguration#VRP} classical solver using the LKH-3 heuristic.
 */
@Component
public class LkhVrpSolver extends VrpSolver {
  private final String scriptDir;
  private final ApplicationContext context;
  private final String solverBinary;

  @Autowired
  public LkhVrpSolver(
      @Value("${custom.lkh.directory}") String scriptDir,
      @Value("${custom.lkh.solver}") String solverBinary,
      ApplicationContext context) {
    this.scriptDir = scriptDir;
    this.solverBinary = solverBinary;
    this.context = context;
  }

  @Override
  public String getName() {
    return "LKH-3 VRP Solver";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver resolver,
      SolvingProperties properties
  ) {

    var solution = new Solution<>(this);

    var processResult = context.getBean(
            PythonProcessRunner.class,
            scriptDir,
            "vrp_lkh.py",
            new String[] {"--lkh-instance", solverBinary}
        )
        .addProblemFilePathToProcessCommand()
        .addSolutionFilePathToProcessCommand("--output-file", "%s")
        .problemFileName("problem.vrp")
        .solutionFileName("problem.sol")
        .run(getProblemType(), solution.getId(), input);

    return Mono.just(processResult.applyTo(solution));
  }
}
