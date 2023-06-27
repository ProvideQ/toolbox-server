package edu.kit.provideq.toolbox.meta.setting;

public class BooleanState extends MetaSolverSetting {
  public boolean state;

  public BooleanState(String name) {
    this(name, false);
  }

  public BooleanState(String name, boolean state) {
    super(name, MetaSolverSettingType.CHECKBOX);

    this.state = state;
  }
}
