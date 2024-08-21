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
 * {@link QuboConfiguration#QUBO} solver using a Qrisps QAOA implementation.
 */
@Component
public class QrispQuboSolver extends QuboSolver {
  private final String vrpPath;
  private final ApplicationContext context;

  @Autowired
  public QrispQuboSolver(
      @Value("${qrisp.directory.vrp}") String vrpPath,
      ApplicationContext context) {
    this.vrpPath = vrpPath;
    this.context = context;
  }

  @Override
  public String getName() {
    return "(Qrisp) QAOA Solver for QUBOs";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver
  ) {
    var solution = new Solution<String>();

    // This value will be passed to the python script,
    // it is used to prevent denial of service issues for large simulations.
    // Default value is 4, higher values are possible but might take much longer to simulate.
    // TODO: allow user to pass a custom gate size as a solver setting
    int maxNumberOfCities = 4;

    var processResult = context.getBean(
            PythonProcessRunner.class,
            vrpPath,
            "qaoa.py",
            new String[] {"%1$s", "--output-file", "%2$s",
                "--size-gate", String.valueOf(maxNumberOfCities)}
        )
        .problemFileName("problem.lp")
        .solutionFileName("problem.bin")
        .run(getProblemType(), solution.getId(), input);

    return Mono.just(processResult.applyTo(solution));
  }
}
