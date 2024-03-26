package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;

/**
 * Data-transfer object to be used for {@link SubRoutineDefinition}s in the REST API.
 */
public record SubRoutineDefinitionDto(
    String typeId,
    String description
) {

  /**
   * Creates a DTO representing the given {@link SubRoutineDefinition}.
   */
  public static SubRoutineDefinitionDto fromSubRoutineDefinition(
      SubRoutineDefinition<?, ?> definition
  ) {
    var typeId = definition.type().getId();
    var description = definition.description();

    return new SubRoutineDefinitionDto(typeId, description);
  }
}
