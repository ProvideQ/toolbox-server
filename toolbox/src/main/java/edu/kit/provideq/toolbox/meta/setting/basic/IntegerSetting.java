package edu.kit.provideq.toolbox.meta.setting.basic;

import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.SolverSettingType;

public class IntegerSetting extends SolverSetting {
  private final int min;
  private final int max;
  private final int value;

  public IntegerSetting(String name, String description, int min, int max) {
    this(false, name, description, min, max);
  }

  public IntegerSetting(boolean required, String name, String description, int min, int max) {
    this(required, name, description, min, max, (max - min) / 2);
  }

  public IntegerSetting(String name, String description, int min, int max, int value) {
    this(false, name, description, min, max, value);
  }

  public IntegerSetting(
      boolean required,
      String name,
      String description,
      int min,
      int max,
      int value) {
    super(name, description, SolverSettingType.INTEGER, required);
    this.min = min;
    this.max = max;
    this.value = value;
  }

  public int getMin() {
    return min;
  }

  public int getMax() {
    return max;
  }

  public int getValue() {
    return value;
  }
}
