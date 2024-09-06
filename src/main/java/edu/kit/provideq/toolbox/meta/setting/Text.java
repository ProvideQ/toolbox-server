package edu.kit.provideq.toolbox.meta.setting;

public class Text extends SolverSetting {
  public String text;

  public Text(String name, String description) {
    this(false, name, description);
  }

  public Text(boolean required, String name, String description) {
    this(required, name, description, "");
  }

  public Text(String name, String description, String text) {
    this(false, name, description, text);
  }

  public Text(boolean required, String name, String description, String text) {
    super(name, description, SolverSettingType.TEXT, required);

    this.text = text;
  }
}
