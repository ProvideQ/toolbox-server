package edu.kit.provideq.toolbox.meta.setting;

public class Text extends MetaSolverSetting {
  public String text;

  public Text(String name, String title) {
    this(name, title, "");
  }

  public Text(String name, String title, String text) {
    super(name, title, MetaSolverSettingType.TEXT);

    this.text = text;
  }
}
