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

  /**
   * Get a setting by name.
   * Note: Make sure to specify the correct type parameter {@code T} for you setting you choose.
   */
  @SuppressWarnings("unchecked")
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
