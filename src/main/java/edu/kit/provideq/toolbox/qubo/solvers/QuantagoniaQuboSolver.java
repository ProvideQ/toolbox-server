package edu.kit.provideq.toolbox.qubo.solvers;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.asbestian.jplex.input.LpFileReader;
import de.asbestian.jplex.input.Variable;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.TextSetting;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link QuboConfiguration#QUBO} solver using the
 * Quantagonia QUBO solver hosted on the PlanQK platform.
 */
@Component
public class QuantagoniaQuboSolver extends QuboSolver {
  private static final String SETTING_PLANQK_TOKEN = "PlanQK Access Token";

  @Override
  public String getName() {
    return "(PlanQK) Quantagonia QUBO Solver";
  }

  @Override
  public List<SolverSetting> getSolverSettings() {
    return List.of(
        new TextSetting(
            SETTING_PLANQK_TOKEN,
            "Create access token as shown in docs: https://docs.planqk.de/services/applications.html"
        )
    );
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties) {
    Optional<String> planQkToken = properties
        .<TextSetting>getSetting(SETTING_PLANQK_TOKEN)
        .map(TextSetting::getText);

    // Return if no token is provided
    if (planQkToken.isEmpty()) {
      var solution = new Solution<>(this);
      solution.setDebugData("No PlanQK token provided.");
      solution.abort();
      return Mono.just(solution);
    }

    // Convert problem data string to buffered reader
    var problemDataReader = new BufferedReader(new StringReader(input));
    // Parse lp data
    LpFileReader lpReader = new LpFileReader(problemDataReader);

    QuantagoniaQuboProblem quantagoniaQubo;
    try {
      quantagoniaQubo = parseQuantagoniaQubo(lpReader);
    } catch (ConversionException e) {
      var solution = new Solution<>(this);
      solution.setDebugData(e.getMessage());
      solution.abort();
      return Mono.just(solution);
    }

    PlanQkApi api = new PlanQkApi();
    return api.call(
        "/quantagonia/quantagonia-s-free-qubo-solver/1.0.0",
        quantagoniaQubo,
        planQkToken.get(),
        new PlanQkApi.ProblemProperties("/v1/hqp/job"),
        new PlanQkApi.StatusProperties<>(
            QuantagoniaQuboStatus.class,
            "/v1/hqp/job/%s/status",
            status -> switch (status) {
              case FINISHED -> PlanQkApi.JobStatus.SUCCEEDED;
              case TERMINATED, ERROR -> PlanQkApi.JobStatus.FAILED;
              case RUNNING, TIMEOUT -> PlanQkApi.JobStatus.RUNNING;
              case CREATED -> PlanQkApi.JobStatus.PENDING;
            }),
        new PlanQkApi.ResultProperties<>(
            QuantagoniaQuboSolution.class,
            "/v1/hqp/job/%s/results"
        )
    ).flatMap(result -> {
      if (result == null) {
        var solution = new Solution<>(this);
        solution.setDebugData("Job couldn't be solved.");
        solution.abort();
        return Mono.just(solution);
      }

      var solution = new Solution<>(this);
      String solutionString = String.join("\n",
          result.getSolution().stream().map(Object::toString).toList());

      solution.setSolutionData(solutionString);
      solution.setDebugData(result.getLog());

      solution.complete();

      return Mono.just(solution);
    });
  }

  private static QuantagoniaQuboProblem parseQuantagoniaQubo(LpFileReader lpReader)
      throws ConversionException {
    var qubo = new QuantagoniaQuboProblem();

    switch (lpReader.getObjective(0).sense()) {
      case MAX:
        qubo.setSense("MAXIMIZE");
        break;
      case MIN:
        qubo.setSense("MINIMIZE");
        break;
      case UNDEF:
        throw new ConversionException("Objective sense is undefined");
      default:
        throw new ConversionException("Unknown objective sense");
    }

    var variables = new HashSet<String>();
    for (Variable variable : lpReader.getContinuousVariables()) {
      // This name is either
      // - a single variable (x0)
      // - a product of variables (x0 * x1)
      // - an exponent of a variable (x0 ^ 2)
      String name = variable.name();

      // Check product
      String[] factors = name.split("\\*");
      if (factors.length > 1) {
        var coefficients = lpReader.getObjective(0).coefficients();
        var coefficient = coefficients.get(name);

        // Get factors (x0 => 0)
        double i = getVariableIndex(factors[0]);
        double j = getVariableIndex(factors[1]);

        variables.add(factors[0].trim());
        variables.add(factors[1].trim());

        // Add quadratic term at i j with coefficient / -4
        qubo.getMatrix().getQuadratic().add(List.of(i, j, coefficient / -4));
      } else {
        // Check exponent
        String[] exponent = name.split("\\^");
        if (exponent.length > 1) {
          var coefficients = lpReader.getObjective(0).coefficients();
          var coefficient = coefficients.get(name);

          // Get factor (x0 => 0)
          double i = Double.parseDouble(exponent[0].trim().replace("x", ""));

          variables.add(exponent[0].trim());

          // Add quadratic term at i i with coefficient / 2
          qubo.getMatrix().getLinear().add(List.of(i, i, Math.abs(coefficient / 2)));
        }

        // Ignore single variable
      }
    }

    qubo.getMatrix().setNumberOfVariables(variables.size());

    return qubo;
  }

  /**
   * Get the index of a variable from its name.
   * Currently only supports variables of the form x0, x1, x2, ...
   *
   * @param variableName The name of the variable
   * @return The index of the variable
   */
  static double getVariableIndex(String variableName) {
    return Double.parseDouble(variableName.trim().replace("x", ""));
  }

  static class QuantagoniaQuboProblem {
    private String sense;
    private Matrix matrix = new Matrix();

    public String getSense() {
      return sense;
    }

    public void setSense(String sense) {
      this.sense = sense;
    }

    public Matrix getMatrix() {
      return matrix;
    }

    public void setMatrix(
        Matrix matrix) {
      this.matrix = matrix;
    }

    public static class Matrix {
      @JsonProperty("n")
      private int numberOfVariables;
      private List<List<Double>> linear = new ArrayList<>();
      private List<List<Double>> quadratic = new ArrayList<>();

      public int getNumberOfVariables() {
        return numberOfVariables;
      }

      public void setNumberOfVariables(int numberOfVariables) {
        this.numberOfVariables = numberOfVariables;
      }

      public List<List<Double>> getLinear() {
        return linear;
      }

      public void setLinear(List<List<Double>> linear) {
        this.linear = linear;
      }

      public List<List<Double>> getQuadratic() {
        return quadratic;
      }

      public void setQuadratic(List<List<Double>> quadratic) {
        this.quadratic = quadratic;
      }
    }
  }

  enum QuantagoniaQuboStatus {
    FINISHED,
    TERMINATED,
    ERROR,
    RUNNING,
    CREATED,
    TIMEOUT
  }

  static class QuantagoniaQuboSolution {
    private String log;
    private String objective;
    private List<Integer> solution;

    public String getLog() {
      return log;
    }

    public void setLog(String log) {
      this.log = log;
    }

    public String getObjective() {
      return objective;
    }

    public void setObjective(String objective) {
      this.objective = objective;
    }

    public List<Integer> getSolution() {
      return solution;
    }

    public void setSolution(List<Integer> solution) {
      this.solution = solution;
    }
  }
}
