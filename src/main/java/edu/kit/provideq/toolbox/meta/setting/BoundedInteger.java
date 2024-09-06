package edu.kit.provideq.toolbox.meta.setting;

public class BoundedInteger extends SolverSetting {
  private final int min;
  private final int max;
  private final int value;

  public BoundedInteger(String name, String description, int min, int max) {
    this(false, name, description, min, max);
  }

  public BoundedInteger(boolean required, String name, String description, int min, int max) {
    this(required, name, description, min, max, (max - min) / 2);
  }

  public BoundedInteger(String name, String description, int min, int max, int value) {
    this(false, name, description, min, max, value);
  }

  public BoundedInteger(
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
