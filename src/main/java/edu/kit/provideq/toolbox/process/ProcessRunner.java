package edu.kit.provideq.toolbox.process;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Runs a process to solve a problem.
 * Accepts a ProcessBuilder to configure the process that should be run.
 * Aspects about the process creation can be modified via builder pattern.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProcessRunner {
  /**
   * The name of the input file that contains the problem data.
   * Note that the executed process NEEDs to use this exact name.
   */
  private static final String PROBLEM_FILE_NAME = "problem";

  /**
   * The name of the output file that contains the solution data.
   * Note that the executed process NEEDs to use this exact name.
   */
  private static final String SOLUTION_FILE_NAME = "solution";

  /**
   * Arguments that are passed to the command line call
   */
  private String[] arguments;

  protected final ProcessBuilder processBuilder;
  protected ResourceProvider resourceProvider;

  private HashMap<String, String> env;

  private String[] problemFilePathCommandFormat;
  private String[] solutionFilePathCommandFormat;
  private String problemFileName = PROBLEM_FILE_NAME;
  private String solutionFileName = SOLUTION_FILE_NAME;

  public ProcessRunner(
          @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
          ProcessBuilder processBuilder,
          String[] arguments) {
    this.processBuilder = processBuilder;
    this.arguments = arguments;
    this.env = new HashMap<>();
  }

  @Autowired
  public void setResourceProvider(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  /**
   * Adds another command to the process builder.
   * This command is the path a file that contains the problem data.
   *
   * @return Returns this instance for chaining.
   */
  public ProcessRunner addProblemFilePathToProcessCommand() {
    return addProblemFilePathToProcessCommand("%s");
  }

  /**
   * Adds another command to the process builder.
   *
   * @param inputPathCommandFormat Format string of the command.
   *                               One %s should be included which will be replaced with
   *                               the path to a file that contains the problem data.
   * @return Returns this instance for chaining.
   */
  public ProcessRunner addProblemFilePathToProcessCommand(String... inputPathCommandFormat) {
    this.problemFilePathCommandFormat = inputPathCommandFormat;

    return this;
  }

  /**
   * Adds another command to the process builder.
   * This command is the path a file that contains the solution data.
   *
   * @return Returns this instance for chaining.
   */
  public ProcessRunner addSolutionFilePathToProcessCommand() {
    return addSolutionFilePathToProcessCommand("%s");
  }

  /**
   * Adds another command to the process builder.
   *
   * @param outputPathCommandFormat Format string of the command.
   *                                One %s should be included which will be replaced with
   *                                the path to a file that contains the solution data.
   * @return Returns this instance for chaining.
   */
  public ProcessRunner addSolutionFilePathToProcessCommand(String... outputPathCommandFormat) {
    this.solutionFilePathCommandFormat = outputPathCommandFormat;

    return this;
  }

  /**
   * Sets a custom name for the file that contains the problem data.
   *
   * @param fileName The name of the file.
   * @return Returns this instance for chaining.
   */
  public ProcessRunner problemFileName(String fileName) {
    this.problemFileName = fileName;

    return this;
  }

  /**
   * Sets a custom name for the file that contains the solution data.
   *
   * @param fileName The name of the file.
   * @return Returns this instance for chaining.
   */
  public ProcessRunner solutionFileName(String fileName) {
    this.solutionFileName = fileName;

    return this;
  }

  /**
   * Runs the process provided in the constructor.
   *
   * @param problemType The type of the problem that is run
   * @param solutionId  The id of the resulting solution
   * @param problemData The problem data that should be solved
   * @return Returns the process result, which contains the solution data
   *     or an error as output depending on the success of the process.
   */
  public ProcessResult<String> run(ProblemType<?, ?> problemType, UUID solutionId, String problemData) {
    return run(problemType, solutionId, problemData, new SimpleProcessResultReader());
  }

  /**
   * Runs the process provided in the constructor.
   *
   * @param problemType The type of the problem that is run
   * @param solutionId  The id of the resulting solution
   * @param problemData The problem data that should be solved
   * @param reader     The reader that retrieves the output of the process
   * @return Returns the process result, which contains the solution data
   *     or an error as output depending on the success of the process.
   */
  public <T> ProcessResult<T> run(ProblemType<?, ?> problemType, UUID solutionId, String problemData, ProcessResultReader<T> reader) {
    // Retrieve the problem directory
    String problemDirectoryPath;
    try {
      problemDirectoryPath = resourceProvider
          .getProblemDirectory(problemType, solutionId)
          .getAbsolutePath();
    } catch (IOException e) {
      return new ProcessResult<T>(
              false,
              Optional.empty(),
              Optional.of("Error: The problem directory couldn't be retrieved:%n%s".formatted(e.getMessage()))
      );
    }

    // Build the problem and solution file paths
    var problemFilePath = Paths.get(problemDirectoryPath, problemFileName);
    var normalizedProblemFilePath = problemFilePath.toString().replace("\\", "/");

    var solutionFile = Paths.get(problemDirectoryPath, solutionFileName);
    var normalizedSolutionFilePath = solutionFile.toString().replace("\\", "/");

    // Write the problem data to the problem file
    try {
      Files.writeString(problemFilePath, problemData);
    } catch (IOException e) {
      return new ProcessResult<T>(
              false,
              Optional.empty(),
              Optional.of("Error: The problem data couldn't be written to %s:%n%s".formatted(
                      normalizedProblemFilePath, e.getMessage()))
      );
    }

    for (String argument : arguments) {
      addCommand(argument.formatted(normalizedProblemFilePath, normalizedSolutionFilePath, problemDirectoryPath));
    }

    for (Entry<String, String> entry : env.entrySet()) {
      addEnvironmentVariableToBuilder(entry.getKey(), entry.getValue());
    }

    // Optionally add the problem file path to the command
    if (problemFilePathCommandFormat != null) {
      for (String format : problemFilePathCommandFormat) {
        addCommand(format.formatted(normalizedProblemFilePath));
      }
    }

    // Optionally add the solution path to the command
    if (solutionFilePathCommandFormat != null) {
      for (String format : solutionFilePathCommandFormat) {
        addCommand(format.formatted(normalizedSolutionFilePath));
      }
    }

    // Run the process
    String processOutput;
    int processExitCode;
    try {
      Process process = processBuilder.start();

      processOutput = resourceProvider.readStream(process.inputReader())
              + resourceProvider.readStream(process.errorReader());

      processExitCode = process.waitFor();
    } catch (IOException | InterruptedException e) {
      return new ProcessResult<T>(
              false,
              Optional.empty(),
              Optional.of(
                      "Solving %s problem resulted in exception:%n%s".formatted(problemType.getId(), e.getMessage())
              )
      );
    }

    // Return prematurely if the process failed
    if (processExitCode != 0) {
      return new ProcessResult<T>(
              false,
              Optional.empty(),
              Optional.of("%s problem couldn't be solved:%n%s".formatted(problemType.getId(), processOutput)));
    }

    // Read the solution file
    ProcessResult<T> result = reader.read(solutionFile, problemFilePath, Path.of(problemDirectoryPath));

    if (!result.success()) {
      return new ProcessResult<T>(
              result.success(),
              result.output(),
              result.errorOutput().isPresent() ? Optional.of(result.errorOutput().get() + "%nCommand Output: %s".formatted(processOutput)) : Optional.empty()
      );
    }

    // Return the solution
    return result;

  }

  private void addCommand(String command) {
    List<String> existingCommands = processBuilder.command();
    existingCommands.add(command);
    processBuilder.command(existingCommands);
  }

  public void addEnvironmentVariable(String key, String value) {
    env.put(key, value);
  }

  private void addEnvironmentVariableToBuilder(String key, String value) {
    processBuilder.environment().put(key, value);
  }

  protected static ProcessBuilder createGenericProcessBuilder(
      String directory,
      String executableName,
      String scriptName,
      String... arguments) {
    String[] commands = new String[arguments.length + 2];
    commands[0] = executableName;
    commands[1] = scriptName;
    System.arraycopy(arguments, 0, commands, 2, arguments.length);

    return new ProcessBuilder()
        .directory(new File(directory))
        .command(commands);
  }
}
