package edu.kit.provideq.toolbox.meta.setting;

public class BoundedDouble extends SolverSetting {
  private final double min;
  private final double max;
  private final double value;

  public BoundedDouble(String name, String description, double min, double max) {
    this(false, name, description, min, max);
  }

  public BoundedDouble(boolean required, String name, String description, double min, double max) {
    this(required, name, description, min, max, (max - min) / 2);
  }

  public BoundedDouble(String name, String description, double min, double max, double value) {
    this(false, name, description, min, max, value);
  }

  public BoundedDouble(
      boolean required,
      String name,
      String description,
      double min,
      double max,
      double value) {
    super(name, description, SolverSettingType.DOUBLE, required);
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
