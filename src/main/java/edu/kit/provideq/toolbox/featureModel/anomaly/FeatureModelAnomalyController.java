package edu.kit.provideq.toolbox.featureModel.anomaly;

import edu.kit.provideq.toolbox.ProblemController;
import edu.kit.provideq.toolbox.ProblemSolverInfo;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.featureModel.anomaly.solvers.*;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;

@RestController
public class FeatureModelAnomalyController extends ProblemController<String, String, FeatureModelAnomalySolver> {

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
  @PostMapping("/solve/featureModel/anomaly")
  public SolutionHandle solve(@RequestBody @Valid SolveFeatureModelAnomalyRequest request) {
    return super.solve(request);
  }

  @GetMapping("/solve/featureModel/anomaly")
  public SolutionHandle getSolution(@RequestParam(name = "id", required = true) long id) {
    return super.getSolution(id);
  }

  @CrossOrigin
  @GetMapping("/solvers/featureModel/anomaly")
  public Set<ProblemSolverInfo> getSolvers() {
    return super.getSolvers();
  }
}
