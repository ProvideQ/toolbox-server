package edu.kit.provideq.toolbox.meta.setting;

public class Checkbox extends MetaSolverSetting {
    public boolean state;

    public Checkbox(String name) {
        this(name, false);
    }

    public Checkbox(String name, boolean state) {
        super(name, MetaSolverSettingType.CHECKBOX);

        this.state = state;
    }
}
