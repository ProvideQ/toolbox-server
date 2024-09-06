package edu.kit.provideq.toolbox.meta.setting;

public class BoundedDouble extends SolverSetting {
  private final double min;
  private final double max;
  private final double value;

  public BoundedDouble(String name, String description, double min, double max) {
    this(name, description, min, max, (max - min) / 2);
  }

  public BoundedDouble(String name, String description, double min, double max, double value) {
    super(name, description, SolverSettingType.DOUBLE);
    this.min = min;
    this.max = max;
    this.value = value;
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  public double getValue() {
    return value;
  }
}
