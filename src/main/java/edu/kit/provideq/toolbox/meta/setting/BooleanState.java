package edu.kit.provideq.toolbox.meta.setting;

public class BooleanState extends MetaSolverSetting {
  public boolean state;

  public BooleanState(String name, String title) {
    this(name, title, false);
  }

  public BooleanState(String name, String title, boolean state) {
    super(name, title, MetaSolverSettingType.CHECKBOX);

    this.state = state;
  }
}
