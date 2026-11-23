package edu.kit.provideq.toolbox.solvers.featuremodel.anomaly.dead;

import edu.kit.provideq.formats.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfFormat;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfSolutionFormat;
import edu.kit.provideq.formats.cnf.dimacs.Variable;
import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.toolbox.meta.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.solvers.featuremodel.FeatureModelFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.solvers.sat.SatConfiguration;
import edu.kit.provideq.toolbox.solvers.sat.SatFamily;
import edu.kit.provideq.toolbox.solvers.sat.SatSolutionFamily;
import java.util.ArrayList;
import java.util.Collection;
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
public class SatBasedDeadFeatureSolver
    implements ProblemSolver<FeatureModelFamily, DeadFeatureSolutionFamily> {
  private static final SubRoutineDefinition<SatFamily, SatSolutionFamily> SAT_SUBROUTINE =
      SubRoutineDefinition.of(
          SatConfiguration.SAT,
          "Called per feature to determine if it is dead",
          false
      );

  @Override
  public String getName() {
    return "SAT-based Dead Feature Solver";
  }

  @Override
  public String getDescription() {
    return "This solver builds SAT formulae to determine dead features in a feature model."
        + " It uses a SAT solver to solve each formula per feature.";
  }

  @Override
  public List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return List.of(SAT_SUBROUTINE);
  }

  @Override
  public Mono<Solution<DeadFeatureSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    // Convert uvl to cnf
    var cnf = convert(formattedInput, DimacsCnfFormat.INSTANCE, conversionService);
    return checkDeadFeatures(cnf, subRoutineResolver, conversionService);
  }

  @Override
  public ProblemType<FeatureModelFamily, DeadFeatureSolutionFamily> getProblemType() {
    return DeadFeatureConfiguration.FEATURE_MODEL_ANOMALY_DEAD;
  }

  private Mono<Solution<DeadFeatureSolutionFamily>> checkDeadFeatures(
      Document<DimacsCnf> dimacsCnf,
      SubRoutineResolver subRoutineResolver,
      ConversionService conversionService
  ) {
    Collection<Variable> variables;
    try {
      variables = dimacsCnf.parse().getVariables();
    } catch (ParseException e) {
      return Mono.error(new ConversionException(
          "Could not parse CNF to get variables: " + e.getMessage(), e));
    }

    return Flux.fromIterable(variables)
        .flatMap(feature -> checkFeatureDead(
            dimacsCnf, feature, subRoutineResolver, conversionService)
          .filter(Boolean.TRUE::equals)
          .map(ignored -> feature.name()))
        .collectList()
        .map(deadFeatures -> {
          var solution = new Solution<>(this);
          solution.setSolutionData(
              new DeadFeatureSolution(deadFeatures), DeadFeatureSolutionFormat.INSTANCE);

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
  private Mono<Boolean> checkFeatureDead(
      Document<DimacsCnf> cnf,
      Variable feature,
      SubRoutineResolver subRoutineResolver,
      ConversionService conversionService
  ) {
    // Use formula: ¬SAT (FM ∧ f) to check for a dead feature
    // So add variable to the cnf of the feature model and check if there is a solution
    // If there is a solution, the feature is not dead

    DimacsCnf featureIsDeadCnf;
    try {
      featureIsDeadCnf = cnf.parse().addOrClause(new ArrayList<>(List.of(feature)));
    } catch (ParseException e) {
      return Mono.error(new ConversionException(
          "Could not add feature to CNF: " + e.getMessage(), e));
    }

    return subRoutineResolver
        .runSubRoutine(SAT_SUBROUTINE,
            FormattedValue.of(Document.of(featureIsDeadCnf), DimacsCnfFormat.INSTANCE),
            conversionService)
        .map(featureIsDeadSolution -> convert(
            featureIsDeadSolution.getSolutionData(),
            DimacsCnfSolutionFormat.INSTANCE,
            conversionService))
        .map(featureIsDeadSolution -> {
          try {
            return featureIsDeadSolution.parse().isVoid();
          } catch (ParseException e) {
            throw new ConversionException(
                "Could not parse SAT solution: " + e.getMessage(), e);
          }
        });
  }
}
