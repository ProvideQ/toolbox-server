package edu.kit.provideq.toolbox.meta.setting;

public class BoundedInteger extends MetaSolverSetting {
  public double min;
  public double max;
  public double value;

  public BoundedInteger(String name, String title, double min, double max) {
    this(name, title, min, max, (max - min) / 2);
  }

  public BoundedInteger(String name, String title, double min, double max, double value) {
    super(name, title, MetaSolverSettingType.RANGE);

    this.min = min;
    this.max = max;
    this.value = value;
  }

}
