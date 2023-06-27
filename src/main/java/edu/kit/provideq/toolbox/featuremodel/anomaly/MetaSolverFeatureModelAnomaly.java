package edu.kit.provideq.toolbox.featuremodel.anomaly;

import edu.kit.provideq.toolbox.featuremodel.anomaly.solvers.FeatureModelAnomalySolver;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Simple {@link MetaSolver} for FeatureModel problems
 */
@Component
public class MetaSolverFeatureModelAnomaly extends MetaSolver<FeatureModelAnomalySolver> {

  @Autowired
  public MetaSolverFeatureModelAnomaly(FeatureModelAnomalySolver anomalySolver) {
    super(anomalySolver);
  }

  @Override
  public FeatureModelAnomalySolver findSolver(Problem problem,
                                              List<MetaSolverSetting> metaSolverSettings) {
    // todo add decision
    return (new ArrayList<>(this.solvers)).get((new Random()).nextInt(this.solvers.size()));
  }
}
