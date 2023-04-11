package edu.kit.provideq.toolbox.featureModel.anomaly;

import edu.kit.provideq.toolbox.ProblemController;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.featureModel.SolveFeatureModelRequest;
import edu.kit.provideq.toolbox.featureModel.anomaly.solvers.*;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@RestController
public class FeatureModelAnomalyController extends ProblemController<FeatureModelAnomalyProblem, String, FeatureModelAnomalySolver> {

  private final MetaSolver<FeatureModelAnomalySolver> metaSolver;

  public FeatureModelAnomalyController(MetaSolver<FeatureModelAnomalySolver> metaSolver) {
    this.metaSolver = metaSolver;
  }

  @Override
  public ProblemType getProblemType() {
    return ProblemType.FEATURE_MODEL_ANOMALY;
  }

  @Override
  public MetaSolver<FeatureModelAnomalySolver> getMetaSolver() {
    return metaSolver;
  }

  @CrossOrigin
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
  public SolutionHandle findFalseOptionalFeatures(@RequestBody @Valid SolveFeatureModelRequest request) {
    return solveAnomaly(request, FeatureModelAnomaly.FALSE_OPTIONAL);
  }

  @CrossOrigin
  @PostMapping("/solve/feature-model/anomaly/redundant-constraints")
  public SolutionHandle findRedundantConstraints(@RequestBody @Valid SolveFeatureModelRequest request) {
    return solveAnomaly(request, FeatureModelAnomaly.REDUNDANT_CONSTRAINTS);
  }

  private SolutionHandle solveAnomaly(SolveFeatureModelRequest request, FeatureModelAnomaly anomaly) {
    return super.solve(request.replaceContent(new FeatureModelAnomalyProblem(request.requestContent, anomaly)));
  }

  @CrossOrigin
  @GetMapping("/solve/feature-model/anomaly/")
  public SolutionHandle getSolution(@RequestParam(name = "id") long id) {
    return super.getSolution(id);
  }

  @CrossOrigin
  @GetMapping("/sub-routines/feature-model/anomaly")
  public List<SubRoutineDefinition> getSubRoutines(@RequestParam(name = "id") String solverId) {
    return super.getSubRoutines(solverId);
  }

  @CrossOrigin
  @GetMapping("/solvers/feature-model/anomaly")
  public Set<ProblemSolverInfo> getSolvers() {
    return super.getSolvers();
  }
}
