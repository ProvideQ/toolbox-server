package edu.kit.provideq.toolbox.meta.setting.basic;

import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.SolverSettingType;

public class DoubleSetting extends SolverSetting {
  private final double min;
  private final double max;
  private final double value;

  public DoubleSetting(String name, String description, double min, double max) {
    this(false, name, description, min, max);
  }

  public DoubleSetting(boolean required, String name, String description, double min, double max) {
    this(required, name, description, min, max, (max - min) / 2);
  }

  public DoubleSetting(String name, String description, double min, double max, double value) {
    this(false, name, description, min, max, value);
  }

  public DoubleSetting(
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
