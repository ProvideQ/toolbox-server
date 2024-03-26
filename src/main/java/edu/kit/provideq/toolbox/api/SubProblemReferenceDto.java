package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import java.util.List;
import java.util.UUID;

/**
 * A sub-problem reference can be used to identify sub-problems corresponding to a referenced
 * sub-routine definition.
 *
 * @param subRoutine the sub-routine that these sub-problems correspond to.
 * @param subProblemIds ID references to the sub-problems.
 */
public record SubProblemReferenceDto(
    SubRoutineDefinitionDto subRoutine,
    List<String> subProblemIds
) {

  /**
   * Creates a DTO for a given sub-routine in a given problem.
   */
  public static SubProblemReferenceDto forSubRoutine(
      Problem<?, ?> problem,
      SubRoutineDefinition<?, ?> subRoutine
  ) {
    var subRoutineDto = SubRoutineDefinitionDto.fromSubRoutineDefinition(subRoutine);
    var subProblemIds = problem.getSubProblems(subRoutine).stream()
        .map(Problem::getId)
        .map(UUID::toString)
        .toList();

    return new SubProblemReferenceDto(subRoutineDto, subProblemIds);
  }
}
