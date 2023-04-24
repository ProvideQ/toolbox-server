package edu.kit.provideq.toolbox.meta.setting;

public abstract class MetaSolverSetting {
    public String name;
    public MetaSolverSettingType type;

    public MetaSolverSetting() {}

    protected MetaSolverSetting(String name, MetaSolverSettingType type) {
        this.name = name;
        this.type = type;
    }
}
