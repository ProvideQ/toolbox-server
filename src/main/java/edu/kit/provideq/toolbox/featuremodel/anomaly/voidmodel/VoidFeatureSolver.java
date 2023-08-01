package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCnf;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class VoidFeatureSolver
    implements ProblemSolver<String, String> {
  @Override
  public String getName() {
    return "SAT-based Void Feature Model Solver";
  }

  @Override
  public List<SubRoutineDefinition> getSubRoutines() {
    return List.of(
        new SubRoutineDefinition(ProblemType.SAT, "sat",
            "Used to find valid configurations in the Feature Model"));
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.FEATURE_MODEL_ANOMALY_VOID;
  }

  @Override
  public void solve(Problem<String> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool) {
    // Convert uvl to cnf
    String cnf;
    try {
      cnf = UvlToDimacsCnf.convert(problem.problemData());
    } catch (ConversionException e) {
      solution.setDebugData("Conversion error: " + e.getMessage());
      return;
    }

    var satSolve = subRoutinePool.<String, DimacsCnfSolution>getSubRoutine(ProblemType.SAT);
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
      solution.abort();
    }
  }
}
