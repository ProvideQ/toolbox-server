package edu.kit.provideq.toolbox.tsp.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.process.PythonProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Classical Solver for the TSP Problem that uses the LKH-3 heuristics.
 */
@Component
public class LkhTspSolver extends TspSolver {

  private final String scriptDir;
  private final ApplicationContext context;

  @Autowired
  public LkhTspSolver(
      /*
       * uses the LKH script dir because it is the same as LKH-3 for VRP
       * (LKH can solve VRP and TSP)
       */
      @Value("${lkh.directory.vrp}") String scriptDir,
      ApplicationContext context) {
    this.scriptDir = scriptDir;
    this.context = context;
  }

  @Override
  public String getName() {
    return "LKH-3 TSP Solver";
  }

  @Override
  public Mono<Solution<String>> solve(String input, SubRoutineResolver subRoutineResolver) {
    var solution = new Solution<String>();

    var processResult = context.getBean(
            PythonProcessRunner.class,
            scriptDir,
            "vrp_lkh.py"
        )
        .addProblemFilePathToProcessCommand()
        .addSolutionFilePathToProcessCommand("--output-file", "%s")
        .problemFileName("problem.vrp")
        .solutionFileName("problem.sol")
        .run(getProblemType(), solution.getId(), input);

    //TODO: write new wrapper that solves TSP problems with LKH-3
    //TODO: change ProcessRunner call to new wrapper

    return Mono.just(processResult.applyTo(solution));
  }
}
