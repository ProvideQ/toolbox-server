package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCnf;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.sat.SatConfiguration;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * This problem solver solves the {@link VoidModelConfiguration#FEATURE_MODEL_ANOMALY_VOID} problem
 * by building {@link SatConfiguration#SAT} formula that is solved by a corresponding solver.
 */
@Component
public class SatBasedVoidFeatureSolver implements ProblemSolver<String, String> {
  private static final SubRoutineDefinition<String, DimacsCnfSolution> SAT_SUBROUTINE =
      new SubRoutineDefinition<>(
          SatConfiguration.SAT,
          "Used to determine if there is any valid configurations of the Feature Model",
          true
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
  public String getDescription() {
    return "This solver builds SAT formulae to determine void features in a feature model. "
        + "It uses a SAT solver to determine if there is any valid configurations of the "
        + "feature model.";
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties
  ) {
    // Convert uvl to cnf
    String cnf;
    try {
      cnf = UvlToDimacsCnf.convert(input);
    } catch (ConversionException e) {
      var solution = new Solution<>(this);
      solution.setDebugData("Conversion error: " + e.getMessage());
      solution.abort();
      return Mono.just(solution);
    }

    return checkVoidFeatureModel(cnf, subRoutineResolver);
  }

  private Mono<Solution<String>> checkVoidFeatureModel(
      String cnf,
      SubRoutineResolver subRoutineResolver
  ) {
    // Check if the feature model is not a void feature model
    return subRoutineResolver.runSubRoutine(SAT_SUBROUTINE, cnf)
        .map(voidSolution -> {
          var solution = new Solution<>(this);
          solution.setDebugData("Dimacs CNF of Feature Model:\n" + cnf);
          // If there is a valid configuration, the feature model is not a void feature model
          var dimacsCnfSolution = voidSolution.getSolutionData();
          if (dimacsCnfSolution == null) {
            solution.setDebugData("No solution found for the feature model.");
            solution.abort();
            return solution;
          }

          solution.setSolutionData(dimacsCnfSolution.isVoid()
              ? "The feature model is a void feature model. The configuration is never valid."
              : "The feature model has valid configurations, for example: \n"
                + dimacsCnfSolution.toHumanReadableString());
          solution.complete();

          return solution;
        });
  }

  @Override
  public ProblemType<String, String> getProblemType() {
    return VoidModelConfiguration.FEATURE_MODEL_ANOMALY_VOID;
  }
}
