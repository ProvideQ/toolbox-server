package edu.kit.provideq.toolbox.meta.setting;

public class IntegerSetting extends MetaSolverSetting {
  public int number;

  public IntegerSetting(String name, String title) {
    this(name, title, 0);
  }

  public IntegerSetting(String name, String title, int defaultValue) {
    super(name, title, MetaSolverSettingType.INTEGER);

    this.number = defaultValue;
  }
}
