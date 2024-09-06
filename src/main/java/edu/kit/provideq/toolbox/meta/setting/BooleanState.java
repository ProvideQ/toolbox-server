package edu.kit.provideq.toolbox.meta.setting;

public class BooleanState extends SolverSetting {
  private boolean state;

  public BooleanState(String name, String description) {
    this(name, description, false);
  }

  public BooleanState(String name, String description, boolean state) {
    super(name, description, SolverSettingType.CHECKBOX);
    this.state = state;
  }

  public boolean getState() {
    return state;
  }

  public void setState(boolean state) {
    this.state = state;
  }
}
