package edu.kit.provideq.toolbox.qubo.solvers;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.asbestian.jplex.input.LpFileReader;
import de.asbestian.jplex.input.Variable;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi.JobInfo;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi.ProblemProperties;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi.ResultProperties;
import edu.kit.provideq.toolbox.integration.planqk.PlanQkApi.StatusProperties;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.basic.TextSetting;
import edu.kit.provideq.toolbox.qubo.QuboConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link QuboConfiguration#QUBO} solver using the
 * Kipu Digitized Counterdiabatic Quantum Optimization (DCQO) solver hosted on the PlanQK platform.
 */
@Component
public class KipuQuboSolver extends QuboSolver {
  private static final String SETTING_PLANQK_TOKEN = "PlanQK Access Token";

  @Override
  public String getName() {
    return "(PlanQK) Kipu QUBO Solver";
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
      return Mono.just(Solution.failed(this, "No PlanQK token provided."));
    }

    KipuQuboProblem kipuQubo;
    LpFileReader lpReader = LpFileReader.fromInput(input);
    try {
      kipuQubo = parseKipuQubo(lpReader);
    } catch (ConversionException e) {
      return Mono.just(Solution.failed(this, e.getMessage()));
    }

    var kipuQuboRequest = new KipuQuboRequest();
    kipuQuboRequest.getData().setOptimization(kipuQubo);

    PlanQkApi api = new PlanQkApi();
    return api.call(
        "/kipu-quantum/kipu-digitized-counterdiabatic-quantum-optimization---dcqo/1.0.0",
        kipuQuboRequest,
        planQkToken.get(),
        new ProblemProperties<>("/", JobInfo.class, JobInfo::getId),
        new StatusProperties<>("/%s", JobInfo.class, JobInfo::getStatus),
        new ResultProperties<>("/%s/result", KipuQuboSolution.class)
    ).flatMap(result -> {
      var solution = new Solution<>(this);
      if (result == null) {
        solution.setDebugData("Job couldn't be solved.");
        solution.abort();
        return Mono.just(solution);
      }

      var entryStrings = result.counts.entrySet()
          .stream()
          .map(x -> x.getKey() + ": " + x.getValue())
          .toList();

      solution.setSolutionData(String.join("\n", entryStrings));
      solution.complete();
      return Mono.just(solution);
    });
  }

  private static KipuQuboProblem parseKipuQubo(LpFileReader lpReader)
      throws ConversionException {
    var qubo = new KipuQuboProblem();

    var binaryVariables = lpReader.getBinaryVariables();
    ToIntFunction<Variable> getVariableIndex = binaryVariables::indexOf;

    // Fill matrix
    var objective = lpReader.getObjective(0);
    for (var term : objective.terms()) {
      var multiplicands = term.multiplicands();
      if (multiplicands.size() != 2) {
        throw new ConversionException("Only quadratic terms are supported");
      }

      int var1 = getVariableIndex.applyAsInt(multiplicands.get(0));
      int var2 = getVariableIndex.applyAsInt(multiplicands.get(1));
      qubo.getCoefficients().put(
          var1 == var2
              ? String.format("(%d,)", var1)
              : String.format("(%d, %d)", var1, var2),
          term.coefficient()
      );
    }

    return qubo;
  }

  static class KipuQuboRequest {
    @JsonProperty("data")
    private KipuQuboData data = new KipuQuboData();

    @JsonProperty("params")
    private KipuQuboParams params = new KipuQuboParams();

    public KipuQuboData getData() {
      return data;
    }

    public void setData(
        KipuQuboData data) {
      this.data = data;
    }

    public KipuQuboParams getParams() {
      return params;
    }

    public void setParams(
        KipuQuboParams params) {
      this.params = params;
    }

    static class KipuQuboData {
      @JsonProperty("optimization")
      private KipuQuboProblem optimization;

      public KipuQuboProblem getOptimization() {
        return optimization;
      }

      public void setOptimization(
          KipuQuboProblem optimization) {
        this.optimization = optimization;
      }
    }

    static class KipuQuboParams {
      @JsonProperty("backend")
      private String backend = "azure.ionq.simulator";

      @JsonProperty("shots")
      private int shots = 1024;

      public String getBackend() {
        return backend;
      }

      public void setBackend(String backend) {
        this.backend = backend;
      }

      public int getShots() {
        return shots;
      }

      public void setShots(int shots) {
        this.shots = shots;
      }
    }
  }

  static class KipuQuboProblem {
    @JsonProperty("coefficients")
    private Map<String, Double> coefficients = new HashMap<>();

    @JsonProperty("annealing_time")
    private double annealingTime = 0.7;

    @JsonProperty("trotter_steps")
    private int trotterSteps = 2;

    @JsonProperty("mode")
    private Mode mode = Mode.CD;

    public Map<String, Double> getCoefficients() {
      return coefficients;
    }

    public void setCoefficients(Map<String, Double> coefficients) {
      this.coefficients = coefficients;
    }

    public double getAnnealingTime() {
      return annealingTime;
    }

    public void setAnnealingTime(Double annealingTime) {
      this.annealingTime = annealingTime;
    }

    public int getTrotterSteps() {
      return trotterSteps;
    }

    public void setTrotterSteps(int trotterSteps) {
      this.trotterSteps = trotterSteps;
    }

    public Mode getMode() {
      return mode;
    }

    public void setMode(
        Mode mode) {
      this.mode = mode;
    }

    enum Mode {
      CD,
      FULL
    }
  }

  static class KipuQuboError {
    private String code;
    private String detail;

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }
  }

  static class KipuQuboSolution {
    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("backend_name")
    private String backendName;

    @JsonProperty("counts")
    private Map<String, Integer> counts;

    @JsonProperty("shots")
    private int shots;

    public String getJobId() {
      return jobId;
    }

    public void setJobId(String jobId) {
      this.jobId = jobId;
    }

    public String getBackendName() {
      return backendName;
    }

    public void setBackendName(String backendName) {
      this.backendName = backendName;
    }

    public Map<String, Integer> getCounts() {
      return counts;
    }

    public void setCounts(Map<String, Integer> counts) {
      this.counts = counts;
    }

    public int getShots() {
      return shots;
    }

    public void setShots(int shots) {
      this.shots = shots;
    }
  }
}
