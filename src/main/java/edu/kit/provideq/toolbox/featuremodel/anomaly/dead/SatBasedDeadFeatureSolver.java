package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCnf;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.toolbox.format.cnf.dimacs.Variable;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.sat.SatConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * This problem solver solves the {@link DeadFeatureConfiguration#FEATURE_MODEL_ANOMALY_DEAD}
 * problem by building {@link SatConfiguration#SAT} formulae that are solved by a corresponding
 * solver.
 */
@Component
public class SatBasedDeadFeatureSolver implements ProblemSolver<String, String> {
  private static final SubRoutineDefinition<String, DimacsCnfSolution> SAT_SUBROUTINE =
      new SubRoutineDefinition<>(
          SatConfiguration.SAT,
          "Called per feature to determine if it is dead"
      );

  @Override
  public String getName() {
    return "SAT-based Dead Feature Solver";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(SAT_SUBROUTINE);
  }

  @Override
  public Mono<Solution<String>> solve(
      String input,
      SubRoutineResolver subRoutineResolver
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

    return checkDeadFeatures(cnf, subRoutineResolver);
  }

  @Override
  public ProblemType<String, String> getProblemType() {
    return DeadFeatureConfiguration.FEATURE_MODEL_ANOMALY_DEAD;
  }

  private Mono<Solution<String>> checkDeadFeatures(
      String cnf,
      SubRoutineResolver subRoutineResolver
  ) {
    // Check if there are any Dead Features
    DimacsCnf dimacsCnf;
    try {
      dimacsCnf = DimacsCnf.fromDimacsCnfString(cnf);
    } catch (ConversionException e) {
      var solution = new Solution<>(this);
      solution.setDebugData("Conversion error: " + e.getMessage());
      solution.abort();
      return Mono.just(solution);
    }

    return Flux.fromIterable(dimacsCnf.getVariables())
        .flatMap(feature -> checkFeatureDead(dimacsCnf, feature, subRoutineResolver)
          .map(isVoid -> Tuples.of(feature, isVoid)))
        .collectMap(Tuple2::getT1, Tuple2::getT2)
        .map(featureIsVoidMap -> {
          var stringBuilder = new StringBuilder();

          for (var entry : featureIsVoidMap.entrySet()) {
            Variable feature = entry.getKey();
            boolean isVoid = entry.getValue();

            if (isVoid) {
              stringBuilder.append(feature.name()).append('\n');
            }
          }

          var solution = new Solution<>(this);
          if (stringBuilder.isEmpty()) {
            solution.setSolutionData("No features are dead features!\n");
          } else {
            solution.setSolutionData("The following features are dead features:\n" + stringBuilder);
          }

          solution.complete();
          return solution;
        });
  }

  /**
   * Checks if a given {@code feature} is dead in a given DIMACS {@code cnf} formula.
   *
   * @param subRoutineResolver used to evaluate a SAT formula for the check.
   * @return the solution of the given {@code feature}.
   *         Use {@link DimacsCnfSolution#isVoid()} to check the feature.
   */
  private static Mono<Boolean> checkFeatureDead(
      DimacsCnf cnf,
      Variable feature,
      SubRoutineResolver subRoutineResolver
  ) {
    // Use formula: ¬SAT (FM ∧ f) to check for a dead feature
    // So add variable to the cnf of the feature model and check if there is a solution
    // If there is a solution, the feature is not dead

    var featureIsDeadCnf = cnf.addOrClause(new ArrayList<>(List.of(feature)));
    return subRoutineResolver
        .runSubRoutine(SAT_SUBROUTINE, featureIsDeadCnf.toString())
        .map(featureIsDeadSolution ->
            DimacsCnfSolution.fromString(cnf, featureIsDeadSolution.getSolutionData().toString()))
        .map(DimacsCnfSolution::isVoid);
  }
}
