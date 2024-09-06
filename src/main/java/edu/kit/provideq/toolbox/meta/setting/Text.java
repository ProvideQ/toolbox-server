package edu.kit.provideq.toolbox.meta.setting;

public class Text extends SolverSetting {
  public String text;

  public Text(String name, String description) {
    this(name, description, "");
  }

  public Text(String name, String description, String text) {
    super(name, description, SolverSettingType.TEXT);

    this.text = text;
  }
}
