package edu.kit.provideq.toolbox.meta.setting;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SolverSettingDeserializer.class)
public abstract class SolverSetting {
  private String name;
  private SolverSettingType type;
  private String description;
  private boolean required;

  protected SolverSetting(
      String name,
      String description,
      SolverSettingType type) {
    this(name, description, type, false);
  }

  protected SolverSetting(
      String name,
      String description,
      SolverSettingType type,
      boolean required) {
    this.setName(name);
    this.setType(type);
    this.setDescription(description);
    this.setRequired(required);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SolverSettingType getType() {
    return type;
  }

  public void setType(SolverSettingType type) {
    this.type = type;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  @Override
  public String toString() {
    return "SolverSetting{"
        + "name=" + name
        + ", type=" + type
        + ", description=" + description
        + ", required=" + required
        + '}';
  }
}
