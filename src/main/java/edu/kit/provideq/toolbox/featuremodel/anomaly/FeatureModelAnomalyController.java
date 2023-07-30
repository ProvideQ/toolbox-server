package edu.kit.provideq.toolbox.featuremodel.anomaly;

import edu.kit.provideq.toolbox.ProblemController;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.featuremodel.anomaly.solvers.FeatureModelAnomalySolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
public class FeatureModelAnomalyController
    extends ProblemController<FeatureModelAnomalyProblem, String, FeatureModelAnomalySolver> {

  private final MetaSolver<
          FeatureModelAnomalyProblem,
          String,
          FeatureModelAnomalySolver> metaSolver;

  public FeatureModelAnomalyController(MetaSolver<
          FeatureModelAnomalyProblem,
          String,
          FeatureModelAnomalySolver> metaSolver) {
    this.metaSolver = metaSolver;
  }

  @Override
  public ProblemType getProblemType() {
    return ProblemType.FEATURE_MODEL_ANOMALY;
  }

  @Override
  public MetaSolver<FeatureModelAnomalyProblem, String, FeatureModelAnomalySolver> getMetaSolver() {
    return metaSolver;
  }

  /*@CrossOrigin
  @PostMapping("/solve/feature-model/anomaly/void")
  public SolutionHandle findVoidFeatureModel(@RequestBody @Valid SolveFeatureModelRequest request) {
    return solveAnomaly(request, FeatureModelAnomaly.VOID);
  }

  @CrossOrigin
  @PostMapping("/solve/feature-model/anomaly/dead")
  public SolutionHandle findDeadFeatures(@RequestBody @Valid SolveFeatureModelRequest request) {
    return solveAnomaly(request, FeatureModelAnomaly.DEAD);
  }

  @CrossOrigin
  @PostMapping("/solve/feature-model/anomaly/false-optional")
  public SolutionHandle findFalseOptionalFeatures(
      @RequestBody @Valid SolveFeatureModelRequest request) {
    return solveAnomaly(request, FeatureModelAnomaly.FALSE_OPTIONAL);
  }

  @CrossOrigin
  @PostMapping("/solve/feature-model/anomaly/redundant-constraints")
  public SolutionHandle findRedundantConstraints(
      @RequestBody @Valid SolveFeatureModelRequest request) {
    return solveAnomaly(request, FeatureModelAnomaly.REDUNDANT_CONSTRAINTS);
  }

  private SolutionHandle solveAnomaly(SolveFeatureModelRequest request,
                                      FeatureModelAnomaly anomaly) {
    var solution = (Solution<String>) super.solve(
        request.replaceContent(new FeatureModelAnomalyProblem(request.requestContent, anomaly)));
    solution.setSolverName(solution.getSolverName() + ": " + anomaly.name);
    return solution; FIXME
  }*/

  @CrossOrigin
  @GetMapping("/solve/feature-model/anomaly/")
  public SolutionHandle getSolution(@RequestParam(name = "id") long id) {
    return super.findSolution(id);
  }

  @CrossOrigin
  @GetMapping("/meta-solver/settings/feature-model/anomaly")
  public List<MetaSolverSetting> getMetaSolverSettings() {
    return metaSolver.getSettings();
  }

  @CrossOrigin
  @GetMapping("/solvers/feature-model/anomaly")
  public Set<ProblemSolverInfo> getSolvers() {
    return super.getSolvers();
  }
}
