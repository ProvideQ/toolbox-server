package edu.kit.provideq.toolbox.process;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Process runner with output post-processing specifically for invoking Python scripts.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PythonProcessRunner extends ProcessRunner {
  /**
   * The name of the file to call for running a GAMS script.
   */
  private static final String PYTHON_EXECUTABLE_NAME
      = "/home/piotr/anaconda3/envs/provideq-toolbox-server/bin/python";

  /**
   * Creates a process runner for a Python script.
   *
   * @param scriptPath the filepath of the Python script to run.
   */
  public PythonProcessRunner(String scriptPath) {
    super(new ProcessBuilder());

    withArguments(PYTHON_EXECUTABLE_NAME, scriptPath);
  }
}
