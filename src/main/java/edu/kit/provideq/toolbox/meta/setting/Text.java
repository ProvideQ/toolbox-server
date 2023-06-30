package edu.kit.provideq.toolbox.meta.setting;

public class Text extends MetaSolverSetting {
  public String text;

  public Text(String name) {
    this(name, "");
  }

  public Text(String name, String text) {
    super(name, MetaSolverSettingType.TEXT);

    this.text = text;
  }
}
