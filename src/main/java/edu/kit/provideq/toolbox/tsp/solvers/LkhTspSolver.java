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

    // the following code will transform the TSP problem into a VRP problem
    // dummy demand and depot sections are added, every city has a demand of 1.
    // this is not an elegant way to solve TSP, but needed to make Lucas' code work.

    // read dimension from input:
    int dimension = 0;
    String[] lines = input.split("\n");
    for (String line : lines) {
      if (line.contains("DIMENSION")) {
        dimension = Integer.parseInt(line.split(":")[1].trim());
        break;
      }
    }
    System.out.println("dimension: " + dimension);
    int capacity = dimension - 1;

    if (input.contains("TYPE : TSP")) {
      input = input.replace("TYPE : TSP", "TYPE : CVRP\nCAPACITY : " + capacity);
    }

    if (!input.contains("DEPOT_SECTION:") && !input.contains("DEMAND_SECTION:")) {
      if (input.contains("EOF")) {
        input = input.replace("EOF", "");
      }
      // add depot section dummy:
      input = input.concat("DEPOT_SECTION:\n1\n-1\n");
      // add dummy for demands:
      input = input.concat("DEMAND_SECTION:\n1 0\n");
      for (int i = 2; i <= dimension; i++) {
        input = input.concat(i + " 1\n");
      }
    }

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

    return Mono.just(processResult.applyTo(solution));
  }
}
