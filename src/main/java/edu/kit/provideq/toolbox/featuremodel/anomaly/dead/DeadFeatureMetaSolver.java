package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

import edu.kit.provideq.toolbox.featuremodel.ExtendedUvlFeatureModel;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * This is the meta solver for the {@link ProblemType#FEATURE_MODEL_ANOMALY_DEAD} problem.
 */
@Component
public class DeadFeatureMetaSolver
    extends MetaSolver<String, String, ProblemSolver<String, String>> {
  public DeadFeatureMetaSolver(SatBasedDeadFeatureSolver solver) {
    super(ProblemType.FEATURE_MODEL_ANOMALY_DEAD, solver);
  }

  @Override
  public ProblemSolver<String, String> findSolver(Problem<String> problem,
                                                  List<MetaSolverSetting> metaSolverSettings) {
    // we only have one solver at this point
    return getAllSolvers().stream().findAny().orElseThrow();
  }

  @Override
  public List<String> getExampleProblems() {
    return ExtendedUvlFeatureModel.getExamples();
  }
}
