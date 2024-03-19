package edu.kit.provideq.toolbox.meta.setting;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = MetaSolverSettingDeserializer.class)
public abstract class MetaSolverSetting {
  public String name;
  public String title;
  public MetaSolverSettingType type;

  public MetaSolverSetting() {
  }

  protected MetaSolverSetting(String name, String title, MetaSolverSettingType type) {
    this.name = name;
    this.type = type;
    this.title = title;
  }
}
