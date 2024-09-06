package edu.kit.provideq.toolbox.meta.setting;

import java.util.List;
import javax.annotation.Nullable;

public class Select<T> extends SolverSetting {
  private List<T> options;
  @Nullable
  private T selectedOption;

  public Select(String name, String description, List<T> options) {
    this(false, name, description, options);
  }

  public Select(boolean required, String name, String description, List<T> options) {
    this(required, name, description, options, null);
  }

  public Select(String name, String description, List<T> options, T selectedOption) {
    this(false, name, description, options, selectedOption);
  }

  public Select(boolean required, String name, String description, List<T> options, T selectedOption) {
    super(name, description, SolverSettingType.SELECT, required);

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
