package edu.kit.provideq.toolbox.meta.setting;

public class IntegerSetting extends MetaSolverSetting {
  private int number;

  public IntegerSetting(String name, String title) {
    this(name, title, 0);
  }

  public IntegerSetting(String name, String title, int defaultValue) {
    super(name, title, MetaSolverSettingType.INTEGER);

    this.number = defaultValue;
  }

  public void setNumber(int number) {
      this.number = number;
  }
  public int getNumber() {
      return number;
  }
}
