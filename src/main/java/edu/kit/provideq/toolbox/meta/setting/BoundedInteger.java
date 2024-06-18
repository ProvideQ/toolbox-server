package edu.kit.provideq.toolbox.meta.setting;

public class BoundedInteger extends MetaSolverSetting {
    private double min;
    private double max;
    private double value;

    public BoundedInteger(String name, String title, double min, double max) {
        this(name, title, min, max, (max - min) / 2);
    }

    public BoundedInteger(String name, String title, double min, double max, double value) {
        super(name, title, MetaSolverSettingType.RANGE);
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
