package edu.kit.provideq.toolbox.meta;

import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import java.util.List;
import java.util.Optional;

public final class SolvingProperties {
  private final List<SolverSetting> settings;

  public SolvingProperties(
      List<SolverSetting> settings
  ) {
    this.settings = settings;
  }

  @SuppressWarnings("unchecked") // caller must ensure correct type
  public <T extends SolverSetting> Optional<T> getSetting(String name) {
    return settings.stream()
        .filter(setting -> setting.getName().equals(name))
        .map(setting -> (T) setting)
        .findFirst();
  }

  @Override
  public String toString() {
    return "SolvingProperties["
        + "settings=" + settings
        + ']';
  }
}
