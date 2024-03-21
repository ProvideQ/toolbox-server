package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCnf;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.sat.SatConfiguration;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * This problem solver solves the {@link VoidModelConfiguration#FEATURE_MODEL_ANOMALY_VOID} problem
 * by building {@link SatConfiguration#SAT} formula that is solved by a corresponding solver.
 */
@Component
public class SatBasedVoidFeatureSolver
    implements ProblemSolver<String, String> {
  @Override
  public String getName() {
    return "SAT-based Void Feature Model Solver";
  }

  @Override
  public List<SubRoutineDefinition> getSubRoutines() {
    return List.of(
        new SubRoutineDefinition(SatConfiguration.SAT,
            "Used to determine if there is any valid configurations of the Feature Model"));
  }

  @Override
  public void solve(String input, Solution<String> solution,
                    SubRoutinePool subRoutinePool) {
    // Convert uvl to cnf
    String cnf;
    try {
      cnf = UvlToDimacsCnf.convert(input);
    } catch (ConversionException e) {
      solution.setDebugData("Conversion error: " + e.getMessage());
      solution.abort();
      return;
    }

    var satSolve = subRoutinePool.<String, DimacsCnfSolution>getSubRoutine(SatConfiguration.SAT);
    checkVoidFeatureModel(solution, cnf, satSolve);
  }

  private static void checkVoidFeatureModel(Solution<String> solution,
                                            String cnf,
                                            Function<String,
                                                Solution<DimacsCnfSolution>> satSolve) {
    // Check if the feature model is not a void feature model
    var voidSolution = satSolve.apply(cnf);

    solution.setDebugData("Dimacs CNF of Feature Model:\n" + cnf);
    if (voidSolution.getStatus() == SolutionStatus.SOLVED) {
      // If there is a valid configuration, the feature model is not a void feature model
      var dimacsCnfSolution = voidSolution.getSolutionData();

      solution.setSolutionData(voidSolution.getSolutionData().isVoid()
          ? "The feature model is a void feature model. The configuration is never valid."
          : "The feature model has valid configurations, for example: \n"
          + dimacsCnfSolution.toHumanReadableString());
      solution.complete();
    } else {
      solution.setDebugData(voidSolution.getDebugData());
      solution.fail();
    }
  }

  @Override
  public ProblemType<String, String> getProblemType() {
    return VoidModelConfiguration.FEATURE_MODEL_ANOMALY_VOID;
  }
}
