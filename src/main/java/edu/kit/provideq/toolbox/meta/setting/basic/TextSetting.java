package edu.kit.provideq.toolbox.meta.setting.basic;

import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.SolverSettingType;

public class TextSetting extends SolverSetting {
  private String text;

  public TextSetting(String name, String description) {
    this(false, name, description);
  }

  public TextSetting(boolean required, String name, String description) {
    this(required, name, description, "");
  }

  public TextSetting(String name, String description, String text) {
    this(false, name, description, text);
  }

  public TextSetting(boolean required, String name, String description, String text) {
    super(name, description, SolverSettingType.TEXT, required);

    this.setText(text);
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
