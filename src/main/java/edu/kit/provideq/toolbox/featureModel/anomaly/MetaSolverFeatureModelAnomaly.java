package edu.kit.provideq.toolbox.featureModel.anomaly;

import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.setting.*;
import org.springframework.beans.factory.annotation.Autowired;
import edu.kit.provideq.toolbox.featureModel.anomaly.solvers.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
