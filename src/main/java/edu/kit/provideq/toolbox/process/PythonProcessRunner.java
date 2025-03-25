package edu.kit.provideq.toolbox.process;

import java.util.List;
import java.util.Optional;
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
   * @param scriptPath the filepath of the Python script to run.
   * @param venvName   the name of the virtual environment to use.
   */
  public PythonProcessRunner(String scriptPath, String venvName) {
    super(new ProcessBuilder());

    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      withArguments(
          String.format("./venv/%s/Scripts/activate.bat", venvName),
          "&&",
          PYTHON_EXECUTABLE_NAME,
          scriptPath);
    } else {
      processBuilder.command(
          "sh",
          "-c",
          String.format(". ./venv/%s/bin/activate && python %s", venvName, scriptPath));
    }
  }

  @Override
  public ProcessRunner withArguments(String... arguments) {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      return super.withArguments(arguments);
    }

    preProcessors.add((problemType, solutionId) -> {
      for (String argument : arguments) {
        for (var transformer : argumentTransformers) {
          argument = transformer.apply(argument);
        }

        if (processBuilder.command().isEmpty()) {
          processBuilder.command(argument);
        } else {
          List<String> existingCommands = processBuilder.command();
          System.out.println(String.format("Adding argument %s to existing commands %s", argument,
              String.join(" ", existingCommands)));
          int lastIndex = existingCommands.size() - 1;
          String lastCommand = existingCommands.get(lastIndex);
          existingCommands.remove(lastIndex);

          var newArgument = lastCommand + " " + argument;
          existingCommands.add(newArgument);
          processBuilder.command(existingCommands);
        }
      }

      return Optional.empty();
    });

    return this;
  }
}
