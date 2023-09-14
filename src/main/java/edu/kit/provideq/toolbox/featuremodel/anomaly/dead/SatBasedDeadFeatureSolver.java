package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.SubRoutinePool;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCnf;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.format.cnf.dimacs.Variable;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * This problem solver solves the {@link ProblemType#FEATURE_MODEL_ANOMALY_DEAD} problem by building
 * {@link ProblemType#SAT} formulae that are solved by a corresponding solver.
 */
@Component
public class SatBasedDeadFeatureSolver implements ProblemSolver<String, String> {
  @Override
  public String getName() {
    return "SAT-based Dead Feature Solver";
  }

  @Override
  public List<SubRoutineDefinition> getSubRoutines() {
    return List.of(
        new SubRoutineDefinition(ProblemType.SAT,
            "Used to find valid configurations in the Feature Model"));
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == ProblemType.FEATURE_MODEL_ANOMALY_DEAD;
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
    checkDeadFeatures(solution, cnf, satSolve);
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

      solution.fail();
    }

    solution.setSolutionData(builder.toString());
    solution.complete();
  }
}
