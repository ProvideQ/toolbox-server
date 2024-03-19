package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCnf;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.featuremodel.anomaly.dead.DeadFeatureManagerConfiguration;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.sat.SatManagerConfiguration;
import edu.kit.provideq.toolbox.test.SubRoutineResolver;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * This problem solver solves the {@link ProblemType#FEATURE_MODEL_ANOMALY_VOID} problem by building
 * {@link SatManagerConfiguration#SAT} formula that is solved by a corresponding solver.
 */
@Component
public class SatBasedVoidFeatureSolver
    implements ProblemSolver<String, String> {
  private static final SubRoutineDefinition<String, DimacsCnfSolution> SAT_SUBROUTINE =
      new SubRoutineDefinition<>(
        SatManagerConfiguration.SAT,
        "Used to determine if there is any valid configurations of the Feature Model"
      );

  @Override
  public String getName() {
    return "SAT-based Void Feature Model Solver";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(SAT_SUBROUTINE);
  }

  @Override
  public boolean canSolve(Problem<String> problem) {
    return problem.type() == VoidFeatureModelManagerConfiguration.FEATURE_MODEL_ANOMALY_VOID;
  }

  @Override
  public Mono<Solution<String>> solve(Problem<String> problem,
                                      SubRoutineResolver subRoutineResolver) {
    var solution = new Solution<String>();

    // Convert uvl to cnf
    String cnf;
    try {
      cnf = UvlToDimacsCnf.convert(problem.problemData());
    } catch (ConversionException e) {
      solution.setDebugData("Conversion error: " + e.getMessage());
      solution.abort();
      return Mono.just(solution);
    }

    return checkVoidFeatureModel(cnf, subRoutineResolver);
  }

  private static Mono<Solution<String>> checkVoidFeatureModel(
      String cnf,
      SubRoutineResolver subRoutineResolver
  ) {
    return subRoutineResolver
        .<String, DimacsCnfSolution>resolve(SAT_SUBROUTINE, cnf)
        .map(voidSolution -> {
          var solution = new Solution<String>();

          solution.setDebugData("Dimacs CNF of Feature Model:\n" + cnf);
          // If there is a valid configuration, the feature model is not a void feature model
          var dimacsCnfSolution = voidSolution.getSolutionData();

          solution.setSolutionData(voidSolution.getSolutionData().isVoid()
              ? "The feature model is a void feature model. The configuration is never valid."
              : "The feature model has valid configurations, for example: \n"
              + dimacsCnfSolution.toHumanReadableString());
          solution.complete();

          return solution;
        });
  }
}
