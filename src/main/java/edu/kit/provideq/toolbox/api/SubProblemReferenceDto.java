package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;
import java.util.List;
import java.util.UUID;

/**
 * A sub-problem reference can be used to identify sub-problems corresponding to a referenced
 * sub-routine definition.
 */
public final class SubProblemReferenceDto {
  private final SubRoutineDefinitionDto subRoutine;
  private final List<String> subProblemIds;


  /**
   * Use {@link #forSubRoutine(Problem, SubRoutineDefinition)}.
   */
  private SubProblemReferenceDto(SubRoutineDefinitionDto subRoutine, List<String> subProblemIds) {
    this.subRoutine = subRoutine;
    this.subProblemIds = subProblemIds;
  }

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

  public SubRoutineDefinitionDto getSubRoutine() {
    return subRoutine;
  }

  public List<String> getSubProblemIds() {
    return subProblemIds;
  }

  @Override
  public String toString() {
    return "SubProblemReferenceDto["
        + "subRoutine=" + subRoutine + ", "
        + "subProblemIds=" + subProblemIds + ']';
  }

}
