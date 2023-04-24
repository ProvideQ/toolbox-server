package edu.kit.provideq.toolbox.meta.setting;

public class Range extends MetaSolverSetting {
    public double min;
    public double max;
    public double value;

    public Range(String name, double min, double max) {
        this(name, min, max, (max - min) / 2);
    }

    public Range(String name, double min, double max, double value) {
        super(name, MetaSolverSettingType.RANGE);

        this.min = min;
        this.max = max;
        this.value = value;
    }
}
