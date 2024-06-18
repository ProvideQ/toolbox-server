package edu.kit.provideq.toolbox.meta.setting;

public class BooleanState extends MetaSolverSetting {
  private boolean state;

  public BooleanState(String name, String title) {
    this(name, title, false);
  }

  public BooleanState(String name, String title, boolean state) {
    super(name, title, MetaSolverSettingType.CHECKBOX);
    this.state = state;
  }

  public boolean getState() {
      return state;
  }

  public void setState(boolean state) {
      this.state = state;
  }
}
