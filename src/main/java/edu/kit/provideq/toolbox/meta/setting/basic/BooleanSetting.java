package edu.kit.provideq.toolbox.meta.setting.basic;

import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.SolverSettingType;

public class BooleanSetting extends SolverSetting {
  private boolean state;

  public BooleanSetting(String name, String description) {
    this(name, description, false);
  }

  public BooleanSetting(boolean required, String name, String description) {
    this(required, name, description, false);
  }

  public BooleanSetting(String name, String description, boolean state) {
    this(false, name, description, state);
  }

  public BooleanSetting(boolean required, String name, String description, boolean state) {
    super(name, description, SolverSettingType.CHECKBOX, required);
    this.state = state;
  }

  public boolean getState() {
    return state;
  }

  public void setState(boolean state) {
    this.state = state;
  }
}
