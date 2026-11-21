package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.formats.core.Format;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.FormatException;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.exception.ConversionException;
import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import java.util.Collections;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * A problem solver provides information about its own suitability to solve a given problem.
 * It can solve problems and write the resulting data in a provided {@link Solution} object.
 *
 * @param <InT>  input format family of the problems this solver can solve.
 * @param <OutT> output format family of the solutions this solver produces.
 */
public interface ProblemSolver<InT extends FormatFamily, OutT extends FormatFamily> {
  /**
   * Returns an id which is unique to the solver.
   *
   * @return id of the solver
   */
  default String getId() {
    return getClass().getSimpleName();
  }

  /**
   * Returns the name of the solver.
   *
   * @return name of the solver
   */
  String getName();

  /**
   * Returns a description of the solver.
   *
   * @return description of the solver
   */
  String getDescription();

  /**
   * Returns the sub problems used to solver this problem.
   *
   * @return list of sub problems
   */
  @SuppressWarnings("java:S1452")
  default List<SubRoutineDefinition<?, ?>> getSubRoutines() {
    return Collections.emptyList();
  }

  /**
   * Returns the solver settings that can be used to configure this solver.
   *
   * @return list of solver settings
   */
  default List<SolverSetting> getSolverSettings() {
    return Collections.emptyList();
  }

  /**
   * Returns the value in the given format's representation, throwing an
   * {@link ConversionException} if the conversion fails. The exception is converted into
   * an aborted {@link Solution} by the central solver invocation in {@code Problem.solve()},
   * so callers do not need to handle conversion failures themselves.
   *
   * @param input             the formatted value to convert
   * @param format            requested representation format
   * @param conversionService service used to derive the representation
   * @param <T>               requested representation type
   * @return the value in the requested representation
   * @throws ConversionException if the value cannot be expressed in the requested format
   */
  default <T> T convert(
      FormattedValue<?> input,
      Format<T> format,
      ConversionService conversionService) {
    try {
      return input.as(format, conversionService);
    } catch (FormatException e) {
      throw new ConversionException("Could not convert input from "
          + input.getFormat().getName() + " to " + format.getName() + ": " + e.getMessage(), e);
    }
  }

  /**
   * Converts the input into the given format and returns it as a {@link FormattedValue}
   * expressed in that format, throwing an {@link ConversionException} if the conversion
   * fails. The exception is converted into an aborted {@link Solution} by the central
   * solver invocation in {@code Problem.solve()}, so callers do not need to handle
   * conversion failures themselves.
   *
   * @param input             the formatted value to convert
   * @param format            requested representation format
   * @param conversionService service used to derive the representation
   * @param <T>               requested representation type
   * @return the converted value as a formatted value in the requested representation
   * @throws ConversionException if the value cannot be expressed in the requested format
   */
  default <T> FormattedValue<T> convertFormatted(
      FormattedValue<?> input,
      Format<T> format,
      ConversionService conversionService) {
    return FormattedValue.of(convert(input, format, conversionService), format);
  }

  /**
   * Solves a given problem instance, current status and final results as well as debug
   * information is stored in the provided {@link Solution} object.
   *
   * @param input the problem instance to solve; representations are pulled from this value
   *        as needed via {@link FormattedValue#as(Format, ConversionService)}.
   * @param subRoutineResolver interface to execute sub-routines with.
   * @return the {@link Solution} in which all resulting information is stored.
   */
  Mono<Solution<OutT>> solve(
      FormattedValue<?> input,
      SubRoutineResolver subRoutineResolver,
      SolvingProperties properties,
      ConversionService conversionService
  );

  /**
   * Returns the problem type that can be solved by this problem solver.
   */
  ProblemType<InT, OutT> getProblemType();
}