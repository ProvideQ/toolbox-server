package edu.kit.provideq.toolbox.process;

import org.springframework.beans.factory.annotation.Autowired;
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
  private static final String PYTHON_EXECUTABLE_NAME = "python";

  /**
   * Creates a process runner for a Python script.
   *
   * @param directory      the working directory to run Python in.
   * @param scriptFileName the filename of the Python script to run.
   */
  public PythonProcessRunner(String directory, String scriptFileName) {
    this(directory, scriptFileName, new String[0]);
  }

  /**
   * Creates a process runner for a Python script.
   *
   * @param directory      the working directory to run Python in.
   * @param scriptFileName the filename of the Python script to run.
   * @param arguments      extra arguments to pass to Python. Use this to pass problem input to the
   *                       solver.
   */
  public PythonProcessRunner(String directory, String scriptFileName, String... arguments) {
    super(
        createGenericProcessBuilder(directory, PYTHON_EXECUTABLE_NAME, scriptFileName), arguments);
  }
}
