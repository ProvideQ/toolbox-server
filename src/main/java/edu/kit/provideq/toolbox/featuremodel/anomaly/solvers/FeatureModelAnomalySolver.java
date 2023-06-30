package edu.kit.provideq.toolbox.featuremodel.anomaly.solvers;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCnf;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.featuremodel.anomaly.FeatureModelAnomalyProblem;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.format.cnf.dimacs.Variable;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class FeatureModelAnomalySolver extends FeatureModelSolver {
  @Override
  public String getName() {
    return "Feature Model Anomaly";
  }

  @Override
  public List<SubRoutineDefinition> getSubRoutines() {
    return List.of(
        new SubRoutineDefinition(ProblemType.SAT, "sat",
            "Used to find valid configurations in the Feature Model"));
  }

  @Override
  public boolean canSolve(Problem<FeatureModelAnomalyProblem> problem) {
    //TODO: assess problemData
    return problem.type() == ProblemType.FEATURE_MODEL_ANOMALY;
  }

  @Override
  public float getSuitability(Problem<FeatureModelAnomalyProblem> problem) {
    //TODO: implement algorithm for suitability calculation
    return 1;
  }

  @Override
  public void solve(Problem<FeatureModelAnomalyProblem> problem, Solution<String> solution,
                    SubRoutinePool subRoutinePool) {
    // Convert uvl to cnf
    String cnf;
    try {
      cnf = UvlToDimacsCnf.convert(problem.problemData().featureModel());
    } catch (ConversionException e) {
      solution.setDebugData("Conversion error: " + e.getMessage());
      return;
    }

    var satSolve = subRoutinePool.<String, DimacsCnfSolution>getSubRoutine(ProblemType.SAT);
    switch (problem.problemData().anomaly()) {
      case VOID -> checkVoidFeatureModel(solution, cnf, satSolve);
      case DEAD -> checkDeadFeatures(solution, cnf, satSolve);
      case FALSE_OPTIONAL, REDUNDANT_CONSTRAINTS -> {
        solution.setDebugData("Not implemented yet!");
        solution.abort();
      }
      default -> {
        solution.setDebugData("Unknown anomaly type " + problem.problemData().anomaly() + "!");
        solution.abort();
      }
    }
  }

  private static void checkDeadFeatures(Solution<String> solution, String cnf,
                                        Function<String, Solution<DimacsCnfSolution>> satSolve) {
    // Check if there are any Dead Features
    DimacsCnf dimacsCnf;
    try {
      dimacsCnf = DimacsCnf.fromDimacsCnfString(cnf);
    } catch (ConversionException e) {
      solution.setDebugData("Conversion error: " + e.getMessage());
      return;
    }

    var builder = new StringBuilder();
    var errorBuilder = new StringBuilder();

    for (Variable variable : dimacsCnf.getVariables()) {
      // Use formula: ¬SAT (FM ∧ f) to check for a dead feature
      // So add variable to the cnf of the feature model and check if there is a solution
      // If there is a solution, the feature is not dead
      var orClause = new ArrayList<Variable>();
      orClause.add(variable);
      var variableCnf = dimacsCnf.addOrClause(orClause);

      var variableSolution = satSolve.apply(variableCnf.toString());

      if (variableSolution.getStatus() == SolutionStatus.SOLVED) {
        var dimacsCnfSolution =
            DimacsCnfSolution.fromString(dimacsCnf, variableSolution.getSolutionData().toString());

        if (dimacsCnfSolution.isVoid()) {
          builder.append(variable.name())
              .append("\n");
        }
      } else {
        errorBuilder.append(variableSolution.getDebugData())
            .append("\n");
      }
    }

    if (builder.isEmpty()) {
      builder.append("No features are dead features!\n");
    } else {
      builder.insert(0, "The following features are dead features:\n");
    }

    if (!errorBuilder.isEmpty()) {
      builder.append("Following errors occurred:\n")
          .append(errorBuilder);

      solution.abort();
    }

    solution.setSolutionData(builder.toString());
    solution.complete();
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
