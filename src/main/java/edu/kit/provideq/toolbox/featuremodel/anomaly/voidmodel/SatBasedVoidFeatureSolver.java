package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import edu.kit.provideq.formats.cnf.dimacs.DimacsCnf;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfFormat;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfSolution;
import edu.kit.provideq.formats.cnf.dimacs.DimacsCnfSolutionFormat;
import edu.kit.provideq.formats.core.Document;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.featuremodel.FeatureModelFamily;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.meta.SolvingProperties;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import edu.kit.provideq.toolbox.meta.SubRoutineResolver;
import edu.kit.provideq.toolbox.sat.SatConfiguration;
import edu.kit.provideq.toolbox.sat.SatFamily;
import edu.kit.provideq.toolbox.sat.SatSolutionFamily;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * This problem solver solves the {@link VoidModelConfiguration#FEATURE_MODEL_ANOMALY_VOID} problem
 * by building {@link SatConfiguration#SAT} formula that is solved by a corresponding solver.
 */
@Component
public class SatBasedVoidFeatureSolver
    implements ProblemSolver<FeatureModelFamily, VoidFeatureSolutionFamily> {
  private static final SubRoutineDefinition<SatFamily, SatSolutionFamily> SAT_SUBROUTINE =
      SubRoutineDefinition.of(
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
  public Mono<Solution<VoidFeatureSolutionFamily>> solve(
      FormattedValue<?> formattedInput,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  ) {
    // Convert uvl to cnf
    var cnf = convertFormatted(formattedInput, DimacsCnfFormat.INSTANCE, conversionService);
    return checkVoidFeatureModel(cnf, subRoutineResolver, conversionService);
  }

  private Mono<Solution<VoidFeatureSolutionFamily>> checkVoidFeatureModel(
      FormattedValue<Document<DimacsCnf>> cnf,
      SubRoutineResolver subRoutineResolver,
      ConversionService conversionService
  ) {
    // Check if the feature model is not a void feature model
    return subRoutineResolver.runSubRoutine(SAT_SUBROUTINE, cnf, conversionService)
        .map(voidSolution -> {
          var solution = new Solution<>(this);
          solution.setDebugData("Dimacs CNF of Feature Model:\n" + cnf.getValue());
          // If there is a valid configuration, the feature model is not a void feature model
          var dimacsCnfSolutionDocument = convert(
              voidSolution.getSolutionData(),
              DimacsCnfSolutionFormat.INSTANCE,
              conversionService);

          DimacsCnfSolution dimacsCnfSolution;
          try {
            dimacsCnfSolution = dimacsCnfSolutionDocument.parse();
          } catch (ParseException e) {
            throw new ConversionException(
                "Could not parse SAT solution: " + e.getMessage(), e);
          }

          boolean voidFeatureModel = dimacsCnfSolution.isVoid();

          solution.setSolutionData(
              new VoidFeatureSolution(voidFeatureModel, () -> dimacsCnfSolution
                  .getVariableMapping()
                  .entrySet().stream()
                  .collect(Collectors.toMap(
                      entry -> entry.getKey().name(),
                      Map.Entry::getValue
                  ))),
              VoidFeatureSolutionFormat.INSTANCE);
          solution.complete();

          return solution;
        });
  }

  @Override
  public ProblemType<FeatureModelFamily, VoidFeatureSolutionFamily> getProblemType() {
    return VoidModelConfiguration.FEATURE_MODEL_ANOMALY_VOID;
  }
}