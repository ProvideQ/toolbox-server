package edu.kit.provideq.toolbox.qubo.solvers;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.asbestian.jplex.input.LpFileReader;
import de.asbestian.jplex.input.Variable;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi.ProblemProperties;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi.ResultProperties;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi.StatusProperties;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.TextSetting;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
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
        new ProblemProperties<>(
            "/v1/hqp/job",
            String.class,
            Function.identity()),
        new StatusProperties<>(
            "/v1/hqp/job/%s/status",
            QuantagoniaQuboStatus.class,
            status -> switch (status) {
              case FINISHED -> PlanQkApi.JobStatus.SUCCEEDED;
              case TERMINATED, ERROR -> PlanQkApi.JobStatus.FAILED;
              case RUNNING, TIMEOUT -> PlanQkApi.JobStatus.RUNNING;
              case CREATED -> PlanQkApi.JobStatus.PENDING;
            }),
        new ResultProperties<>(
            "/v1/hqp/job/%s/results",
            QuantagoniaQuboSolution.class)
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

    var objective = lpReader.getObjective(0);
    var sense = switch (objective.sense()) {
      case MAX -> "MAXIMIZE";
      case MIN -> "MINIMIZE";
      case UNDEF -> throw new ConversionException("Objective sense is undefined");
    };
    qubo.setSense(sense);

    // Map variable names to digit identifiers which Quantagonia expects
    var binaryVariables = lpReader.getBinaryVariables();
    ToIntFunction<Variable> getVariableIndex = binaryVariables::indexOf;

    // Fill matrix
    for (var term : objective.terms()) {
      var coefficient = term.coefficient();
      var multiplicands = term.multiplicands();
      if (multiplicands.size() != 2) {
        throw new ConversionException("Only quadratic terms are supported");
      }

      var var1 = getVariableIndex.applyAsInt(multiplicands.get(0));
      var var2 = getVariableIndex.applyAsInt(multiplicands.get(1));

      if (var1 == var2) {
        // Add term in the diagonal with coefficient / 2
        qubo.getMatrix().getLinear().add(List.of(var1, var2, Math.abs(coefficient / 2)));
      } else {
        // Add term at i j with coefficient / -4
        qubo.getMatrix().getQuadratic().add(List.of(var1, var2, coefficient / -4));
      }

    }

    qubo.getMatrix().setNumberOfVariables(binaryVariables.size());

    return qubo;
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
      private List<List<Number>> linear = new ArrayList<>();
      private List<List<Number>> quadratic = new ArrayList<>();

      public int getNumberOfVariables() {
        return numberOfVariables;
      }

      public void setNumberOfVariables(int numberOfVariables) {
        this.numberOfVariables = numberOfVariables;
      }

      public List<List<Number>> getLinear() {
        return linear;
      }

      public void setLinear(List<List<Number>> linear) {
        this.linear = linear;
      }

      public List<List<Number>> getQuadratic() {
        return quadratic;
      }

      public void setQuadratic(List<List<Number>> quadratic) {
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
