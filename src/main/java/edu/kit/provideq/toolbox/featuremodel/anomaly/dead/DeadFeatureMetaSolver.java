package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeadFeatureMetaSolver extends MetaSolver<String, String, DeadFeatureSolver> {
  private final DeadFeatureSolver solver = new DeadFeatureSolver();

  public DeadFeatureMetaSolver(DeadFeatureSolver solver) {
    super(ProblemType.FEATURE_MODEL_ANOMALY_DEAD, solver);
  }

  @Override
  public DeadFeatureSolver findSolver(Problem<String> problem,
                                      List<MetaSolverSetting> metaSolverSettings) {
    // we only have one solver at this point
    return solver;
  }
}
