package edu.kit.provideq.toolbox.meta.setting.basic;

import edu.kit.provideq.toolbox.meta.setting.SolverSetting;
import edu.kit.provideq.toolbox.meta.setting.SolverSettingType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;

public class SelectSetting<T> extends SolverSetting {
  private List<String> options;
  @Nullable
  private String selectedOption;

  public SelectSetting(
      String name,
      String description,
      List<T> options,
      Function<T, String> mapper) {
    this(false, name, description, options, mapper);
  }

  public SelectSetting(
      boolean required,
      String name,
      String description,
      List<T> options,
      Function<T, String> mapper) {
    this(required, name, description, options, null, mapper);
  }

  public SelectSetting(
      String name,
      String description, List<T> options,
      T selectedOption,
      Function<T, String> mapper) {
    this(false, name, description, options, selectedOption, mapper);
  }

  public SelectSetting(
      boolean required,
      String name,
      String description,
      List<T> options,
      T selectedOption,
      Function<T, String> mapper) {
    super(name, description, SolverSettingType.SELECT, required);

    this.setOptions(options, mapper);
    this.setSelectedOption(selectedOption, mapper);
  }

  public List<String> getOptions() {
    return Collections.unmodifiableList(options);
  }

  public List<T> getOptionsT(Function<String, T> mapper) {
    Objects.requireNonNull(mapper);

    return options.stream()
        .map(mapper)
        .toList();
  }

  public void setOptions(List<T> options, Function<T, String> mapper) {
    Objects.requireNonNull(mapper);

    this.options = options.stream()
        .map(mapper)
        .toList();
  }

  @Nullable
  public String getSelectedOption() {
    return selectedOption;
  }

  @Nullable
  public T getSelectedOptionT(Function<String, T> mapper) {
    Objects.requireNonNull(mapper);

    return mapper.apply(selectedOption);
  }

  public void setSelectedOption(@Nullable T selectedOption, Function<T, String> mapper) {
    Objects.requireNonNull(mapper);

    this.selectedOption = mapper.apply(selectedOption);
  }
}
