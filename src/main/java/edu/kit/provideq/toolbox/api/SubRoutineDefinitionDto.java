package edu.kit.provideq.toolbox.api;

import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;

/**
 * Data-transfer object to be used for {@link SubRoutineDefinition}s in the REST API.
 */
public final class SubRoutineDefinitionDto {
  private final String typeId;
  private final String description;

  /**
   * Internal constructor, used for de-serialization.
   */
  @SuppressWarnings("unused")
  private SubRoutineDefinitionDto() {
    this.typeId = null;
    this.description = null;
  }

  /**
   * Use {@link #fromSubRoutineDefinition(SubRoutineDefinition)}.
   */
  private SubRoutineDefinitionDto(
      String typeId,
      String description
  ) {
    this.typeId = typeId;
    this.description = description;
  }

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

  public String getTypeId() {
    return typeId;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "SubRoutineDefinitionDto["
        + "typeId=" + typeId + ", "
        + "description=" + description + ']';
  }
}
