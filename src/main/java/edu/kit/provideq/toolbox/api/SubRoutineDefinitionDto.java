package edu.kit.provideq.toolbox.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.provideq.toolbox.meta.SubRoutineDefinition;

/**
 * Data-transfer object to be used for {@link SubRoutineDefinition}s in the REST API.
 */
public final class SubRoutineDefinitionDto {
  private final String typeId;
  private final String description;
  private final boolean isCalledOnlyOnce;

  /**
   * Use {@link #fromSubRoutineDefinition(SubRoutineDefinition)}.
   *
   * @param typeId           the id of the sub-routine's problem type
   * @param description      the sub-routine's description
   * @param isCalledOnlyOnce whether the sub-routine is called at most once
   */
  @JsonCreator
  private SubRoutineDefinitionDto(
      @JsonProperty("typeId") String typeId,
      @JsonProperty("description") String description,
      @JsonProperty("isCalledOnlyOnce") boolean isCalledOnlyOnce
  ) {
    this.typeId = typeId;
    this.description = description;
    this.isCalledOnlyOnce = isCalledOnlyOnce;
  }

  /**
   * Creates a DTO representing the given {@link SubRoutineDefinition}.
   */
  public static SubRoutineDefinitionDto fromSubRoutineDefinition(
      SubRoutineDefinition<?, ?> definition
  ) {
    var typeId = definition.type().getId();
    var description = definition.description();
    var isCalledOnlyOnce = definition.isCalledOnlyOnce();

    return new SubRoutineDefinitionDto(typeId, description, isCalledOnlyOnce);
  }

  public String getTypeId() {
    return typeId;
  }

  public String getDescription() {
    return description;
  }

  public boolean getIsCalledOnlyOnce() {
    return isCalledOnlyOnce;
  }

  @Override
  public String toString() {
    return "SubRoutineDefinitionDto["
        + "typeId=" + typeId + ", "
        + "description=" + description + ", "
        + "isCalledOnlyOnce=" + isCalledOnlyOnce + ']';
  }
}
