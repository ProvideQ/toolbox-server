package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * This is the meta solver for the {@link ProblemType#FEATURE_MODEL_ANOMALY_VOID} problem.
 */
@Component
public class VoidFeatureMetaSolver
    extends MetaSolver<String, String, ProblemSolver<String, String>> {
  public VoidFeatureMetaSolver(SatBasedVoidFeatureSolver solver) {
    super(ProblemType.FEATURE_MODEL_ANOMALY_VOID, solver);
  }

  @Override
  public ProblemSolver<String, String> findSolver(Problem<String> problem,
                                                  List<MetaSolverSetting> metaSolverSettings) {
    // we only have one solver at this point
    return getAllSolvers().stream().findAny().orElseThrow();
  }
}
