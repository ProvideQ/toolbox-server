package edu.kit.provideq.toolbox.qubo.solvers;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.asbestian.jplex.input.LpFileReader;
import de.asbestian.jplex.input.Variable;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.authentication.AuthenticationOptions;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkRunner;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolveOptions;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.eclipse.collections.api.map.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link ProblemType#QUBO} solver using the Quantagonia QUBO solver hosted on the PlanQK platform.
 */
@Component
public class QuantagoniaQuboSolver extends QuboSolver {
  private final String quboPath;

  private final ApplicationContext context;

  @Autowired
  public QuantagoniaQuboSolver(
      @Value("${qiskit.directory.qubo}") String quboPath,
      ApplicationContext context) {
    this.quboPath = quboPath;
    this.context = context;
  }

  @Override
  public AuthenticationOptions getAuthenticationOptions() {
    return new AuthenticationOptions(
        "PlanQK",
        "PlanQK Docs: https://docs.platform.planqk.de/applications.html",
        true);
  }

  @Override
  public String getName() {
    return "Quantagonia QUBO via PlanQK";
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.QUBO;
  }

  @Override
  public Mono<Solution<String>> solve(Problem<String> problem,
                                      Solution<String> solution,
                                      SolveOptions solveOptions) {
    // Check if token is provided
    if (solveOptions.authentication() == null || solveOptions.authentication().token() == null) {
      solution.setDebugData("No token provided");
      solution.abort();
      return Mono.just(solution);
    }

    // Convert problem data string to buffered reader
    var problemDataReader = new BufferedReader(new StringReader(problem.problemData()));

    // Parse lp data
    LpFileReader lpReader = new LpFileReader(problemDataReader);

    // Convert lp to quantagonia qubo problem
    var quboProblem = new QuantagoniaQuboProblem();
    switch (lpReader.getObjective(0).sense()) {
      case MAX:
        quboProblem.sense = "MAXIMIZE";
        break;
      case MIN:
        quboProblem.sense = "MINIMIZE";
        break;
      case UNDEF:
        solution.setDebugData("Objective sense is undefined");
        solution.abort();
        return Mono.just(solution);
      default:
        throw new IllegalArgumentException();
    }

    var variables = new HashSet<String>();
    for (Variable variable : lpReader.getContinuousVariables()) {
      System.out.println(variable.name());

      // This name is either
      // - a single variable (x0)
      // - a product of variables (x0 * x1)
      // - an exponent of a variable (x0 ^ 2)
      String name = variable.name();

      // Check product
      String[] factors = name.split("\\*");
      if (factors.length > 1) {
        ImmutableMap<String, Double> coefficients = lpReader.getObjective(0).coefficients();
        var coefficient = coefficients.get(name);

        // Get factors (x0 => 0)
        double i = getVariableIndex(factors[0]);
        double j = getVariableIndex(factors[1]);

        variables.add(factors[0].trim());
        variables.add(factors[1].trim());

        // Add quadratic term at i j with coefficient / -4
        quboProblem.matrix.quadratic.add(List.of(i, j, coefficient / -4));
      } else {
        // Check exponent
        String[] exponent = name.split("\\^");
        if (exponent.length > 1) {
          ImmutableMap<String, Double> coefficients = lpReader.getObjective(0).coefficients();
          var coefficient = coefficients.get(name);

          // Get factor (x0 => 0)
          double i = Double.parseDouble(exponent[0].trim().replace("x", ""));

          variables.add(exponent[0].trim());

          // Add quadratic term at i i with coefficient / 2
          quboProblem.matrix.linear.add(List.of(i, i, Math.abs(coefficient / 2)));
        }

        // Ignore single variable
      }
    }

    quboProblem.matrix.numberOfVariables = variables.size();

    // Run Quantagonia QUBO solver via PlanQK
    return context.getBean(
            PlanQkRunner.class,
            quboPath)
        .problemProperties(new PlanQkRunner.ProblemProperties("/v1/hqp/job"))
        .statusProperties(
            new PlanQkRunner.StatusProperties<>(
                QuantagoniaQuboStatus.class,
                "/v1/hqp/job/%s/status",
                status -> switch (status) {
                  case Finished -> PlanQkApi.JobStatus.SUCCEEDED;
                  case Terminated, Error -> PlanQkApi.JobStatus.FAILED;
                  case Running, Timeout -> PlanQkApi.JobStatus.RUNNING;
                  case Created -> PlanQkApi.JobStatus.PENDING;
                })
        )
        .solutionProperties(new PlanQkRunner.SolutionProperties<>(
            QuantagoniaQuboSolution.class,
            "/v1/hqp/job/%s/results"
        ))
        .addAuthentication(solveOptions.authentication().token())
        .run(
            "/quantagonia/quantagonia-s-free-qubo-solver/1.0.0",
            quboProblem,
            QuantagoniaQuboSolution.class)
        .flatMap(solutionResult -> {
          // Return if process failed
          if (!solutionResult.success()) {
            solution.setDebugData(solutionResult.output());
            solution.abort();
            return Mono.just(solution);
          }

          String solutionString = String.join("\n",
              solutionResult.solution().solution.stream().map(Object::toString).toList());

          solution.setSolutionData(solutionString);
          solution.setDebugData(solutionResult.solution().log);

          solution.complete();

          return Mono.just(solution);
        });
  }

  /**
   * Get the index of a variable from its name.
   * Currently only supports variables of the form x0, x1, x2, ...
   *
   * @param variableName The name of the variable
   * @return The index of the variable
   */
  double getVariableIndex(String variableName) {
    return Double.parseDouble(variableName.trim().replace("x", ""));
  }

  static class QuantagoniaQuboProblem {
    public String sense;

    public Matrix matrix = new Matrix();

    public static class Matrix {
      @JsonProperty("n")
      public int numberOfVariables;

      public List<List<Double>> linear = new ArrayList<>();

      public List<List<Double>> quadratic = new ArrayList<>();
    }
  }

  enum QuantagoniaQuboStatus {
    Finished,
    Terminated,
    Error,
    Running,
    Created,
    Timeout
  }

  static class QuantagoniaQuboSolution {
    public String log;
    public String objective;
    public List<Integer> solution;
  }
}
