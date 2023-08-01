package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VoidFeatureMetaSolver
    extends MetaSolver<String, String, ProblemSolver<String, String>> {
  private final VoidFeatureSolver solver;

  public VoidFeatureMetaSolver(VoidFeatureSolver solver) {
    super(ProblemType.FEATURE_MODEL_ANOMALY_VOID, solver);
    this.solver = solver;
  }

  @Override
  public ProblemSolver<String, String> findSolver(Problem<String> problem,
                                                  List<MetaSolverSetting> metaSolverSettings) {
    return this.solver;
  }
}
