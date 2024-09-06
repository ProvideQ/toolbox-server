package edu.kit.provideq.toolbox.meta.setting;

import java.util.List;
import javax.annotation.Nullable;

public class Select<T> extends SolverSetting {
  private List<T> options;
  @Nullable
  private T selectedOption;

  public Select(String name, String description, List<T> options) {
    this(name, description, options, null);
  }

  public Select(String name, String description, List<T> options, T selectedOption) {
    super(name, description, SolverSettingType.SELECT);

    this.setOptions(options);
    this.setSelectedOption(selectedOption);
  }

  public List<T> getOptions() {
    return options;
  }

  public void setOptions(List<T> options) {
    this.options = options;
  }

  @Nullable
  public T getSelectedOption() {
    return selectedOption;
  }

  public void setSelectedOption(@Nullable T selectedOption) {
    this.selectedOption = selectedOption;
  }
}
