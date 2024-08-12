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
        .run(getProblemType(), solution.getId(), adaptInput(input));

    return Mono.just(processResult.applyTo(solution));
  }

  /**
   * The following code will transform the TSP problem into a VRP problem
   * dummy demand and depot sections are added, every city has a demand of 1.
   * This is not an elegant way to solve TSP, but needed to make Lucas' code work.
   *
   * @param originalInput original input of the TSP problem
   * @return reduction to a VRP
   */
  private String adaptInput(String originalInput) {
    String inputAsVrp = originalInput;

    //read dimension:
    int dimension = 0;
    String[] lines = inputAsVrp.split("\n");
    for (String line : lines) {
      if (line.contains("DIMENSION")) {
        dimension = Integer.parseInt(line.split(":")[1].trim());
        break;
      }
    }
    int capacity = dimension - 1;

    //change problem type to vrp:
    if (inputAsVrp.contains("TYPE : TSP")) {
      inputAsVrp = inputAsVrp.replace("TYPE : TSP", "TYPE : CVRP\nCAPACITY : " + capacity);
    }

    //check if depot and demand section are gives (should not be the case when tsp)
    if (!inputAsVrp.contains("DEPOT_SECTION:") && !inputAsVrp.contains("DEMAND_SECTION:")) {
      if (inputAsVrp.contains("EOF")) {
        inputAsVrp = inputAsVrp.replace("EOF", "");
      }
      // add depot section dummy:
      inputAsVrp = inputAsVrp.concat("DEPOT_SECTION:\n1\n-1\n");
      // add dummy for demands:
      inputAsVrp = inputAsVrp.concat("DEMAND_SECTION:\n1 0\n");
      for (int i = 2; i <= dimension; i++) {
        inputAsVrp = inputAsVrp.concat(i + " 1\n");
      }
    }

    return inputAsVrp;
  }
}
