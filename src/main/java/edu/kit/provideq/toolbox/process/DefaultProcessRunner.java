package edu.kit.provideq.toolbox.process;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Generic process runner for any process.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DefaultProcessRunner extends ProcessRunner {
  /**
   * Creates a process runner to run any process.
   */
  @Autowired
  public DefaultProcessRunner() {
    super(new ProcessBuilder());
  }
}
