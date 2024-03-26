package edu.kit.provideq.toolbox.featuremodel.anomaly.dead;

import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.convert.UvlToDimacsCnf;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.toolbox.format.cnf.dimacs.DimacsCnfSolution;
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
      var solution = new Solution<String>();
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

  private static Mono<Solution<String>> checkDeadFeatures(
      String cnf,
      SubRoutineResolver subRoutineResolver
  ) {
    // Check if there are any Dead Features
    DimacsCnf dimacsCnf;
    try {
      dimacsCnf = DimacsCnf.fromDimacsCnfString(cnf);
    } catch (ConversionException e) {
      var solution = new Solution<String>();
      solution.setDebugData("Conversion error: " + e.getMessage());
      solution.abort();
      return Mono.just(solution);
    }

    return Flux.fromIterable(dimacsCnf.getVariables())
        .flatMap(variable -> {
          // Use formula: ¬SAT (FM ∧ f) to check for a dead feature
          // So add variable to the cnf of the feature model and check if there is a solution
          // If there is a solution, the feature is not dead

          var variableCheckCnf = dimacsCnf.addOrClause(new ArrayList<>(List.of(variable)));
          return subRoutineResolver
              .runSubRoutine(SAT_SUBROUTINE, variableCheckCnf.toString())
              .map(variableCheckSolution -> DimacsCnfSolution.fromString(dimacsCnf, variableCheckSolution.getSolutionData().toString()))
              .flatMap(dimacsCnfSolution -> {
                if (dimacsCnfSolution.isVoid()) {
                  return Mono.just(variable);
                } else {
                  return Mono.empty();
                }
              });
        })
        .reduceWith(
            StringBuilder::new,
            (builder, variable) -> builder.append(variable.name()).append('\n')
        )
        .map(builder -> {
          if (builder.isEmpty()) {
            builder.append("No features are dead features!\n");
          } else {
            builder.insert(0, "The following features are dead features:\n");
          }

          var solution = new Solution<String>();
          solution.setSolutionData(builder.toString());
          solution.complete();
          return solution;
        });
  }
}
