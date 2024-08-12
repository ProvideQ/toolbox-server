package edu.kit.provideq.toolbox.meta.setting;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = MetaSolverSettingDeserializer.class)
public abstract class MetaSolverSetting {
  private String name;
  private MetaSolverSettingType type;
  private String title;

  protected MetaSolverSetting(String name, String title, MetaSolverSettingType type) {
    this.setName(name);
    this.setType(type);
    this.setTitle(title);
  }

  public String getTitle() {
    return title;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public MetaSolverSettingType getType() {
    return type;
  }

  public void setType(MetaSolverSettingType type) {
    this.type = type;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
