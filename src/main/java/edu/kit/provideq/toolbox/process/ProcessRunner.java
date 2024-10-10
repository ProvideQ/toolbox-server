package edu.kit.provideq.toolbox.process;

import edu.kit.provideq.toolbox.ResourceProvider;
import edu.kit.provideq.toolbox.meta.ProblemType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
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
  public static final String PROBLEM_DIRECTORY_PATH = "PROBLEM_DIRECTORY_PATH";
  public static final String INPUT_FILE_PATH = "INPUT_FILE_PATH";
  public static final String OUTPUT_FILE_PATH = "OUTPUT_FILE_PATH";

  /**
   * The default name of the input file.
   * Note that the executed process NEEDs to use this exact name.
   */
  private static final String INPUT_FILE_NAME = "input";

  /**
   * The default name of the output file.
   * Note that the executed process NEEDs to use this exact name.
   */
  private static final String OUTPUT_FILE_NAME = "output";

  protected final ProcessBuilder processBuilder;
  protected ResourceProvider resourceProvider;

  private final List<BiFunction<ProblemType<?, ?>, UUID, Optional<Exception>>> preProcessors;
  private final List<BiFunction<ProblemType<?, ?>, UUID, Optional<Exception>>> postProcessors;
  private final List<UnaryOperator<String>> argumentTransformers;

  /**
   * The directory of the problem that is being solved.
   * Only set when the run method is called.
   */
  private String problemDirectory;

  public ProcessRunner(ProcessBuilder processBuilder) {
    this.processBuilder = processBuilder;
    this.preProcessors = new ArrayList<>();
    this.postProcessors = new ArrayList<>();
    this.argumentTransformers = new ArrayList<>();
    argumentTransformers.add(x -> x.replace(PROBLEM_DIRECTORY_PATH, problemDirectory));
  }

  protected static ProcessBuilder createGenericProcessBuilder(
      String executableName,
      String directory,
      String scriptName) {
    String[] commands = new String[2];
    commands[0] = executableName;
    commands[1] = scriptName;

    return new ProcessBuilder()
        .directory(new File(directory))
        .command(commands);
  }

  @Autowired
  public void setResourceProvider(ResourceProvider resourceProvider) {
    this.resourceProvider = resourceProvider;
  }

  /**
   * Writes the input data to a default input file.
   *
   * @param inputData The input data to be written to the input file.
   * @return ProcessRunner instance for chaining.
   */
  public ProcessRunner writeInputFile(String inputData) {
    return writeInputFile(inputData, INPUT_FILE_NAME);
  }

  /**
   * Writes the input data to a specific input file.
   *
   * @param inputData The input data to be written to the input file.
   * @param inputFileName The name of the file to write the input data to.
   * @return ProcessRunner instance for chaining.
   */
  public ProcessRunner writeInputFile(String inputData, String inputFileName) {
    // Add at the beginning of the pre-processors list
    // This ensures that the argument transformers are applied for every argument
    preProcessors.add(0, (problemType, solutionId) -> {
      var inputFilePath = Paths.get(problemDirectory, inputFileName);
      var normalizedInputFilePath = inputFilePath.toString().replace("\\", "/");

      // Write the input data to an input file
      try {
        Files.writeString(inputFilePath, inputData);
      } catch (IOException e) {
        return Optional.of(new IOException(
            "Error: The input data couldn't be written to %s:%n%s".formatted(
                normalizedInputFilePath, e.getMessage()),
            e));
      }

      // Add support for replacing the input file path in the arguments
      argumentTransformers.add(
          argument -> argument.replace(INPUT_FILE_PATH, normalizedInputFilePath));

      return Optional.empty();
    });

    return this;
  }

  /**
   * Reads the output data from the process console output.
   *
   * @return ProcessRunner instance for chaining.
   */
  public ProcessRunnerExecutor<String> readOutputString() {
    return getExecutor((processOutput, processError) -> new ProcessResult<>(
            true,
            Optional.of(processOutput),
            Optional.empty()
        )
    );
  }

  /**
   * Reads the output data from a default output file.
   *
   * @return ProcessRunner instance for chaining.
   */
  public ProcessRunnerExecutor<String> readOutputFile() {
    return readOutputFile(OUTPUT_FILE_NAME);
  }

  /**
   * Reads the output data from a specific output file.
   *
   * @param outputFileName The name of the file to read the output data from.
   * @return ProcessRunner instance for chaining.
   */
  public ProcessRunnerExecutor<String> readOutputFile(String outputFileName) {
    return readOutputFile(outputFileName, new SimpleProcessResultReader());
  }

  /**
   * Reads the output data from a default output file using a custom reader.
   *
   * @param reader The reader to use to read the output data.
   * @param <T> The type of the output data.
   * @return ProcessRunner instance for chaining.
   */
  public <T> ProcessRunnerExecutor<T> readOutputFile(ProcessResultReader<T> reader) {
    return readOutputFile(OUTPUT_FILE_NAME, reader);
  }

  /**
   * Reads the output data from a specific output file using a custom reader.
   *
   * @param outputFileName The name of the file to read the output data from.
   * @param reader The reader to use to read the output data.
   * @param <T> The type of the output data.
   * @return ProcessRunner instance for chaining.
   */
  public <T> ProcessRunnerExecutor<T> readOutputFile(String outputFileName,
                                                     ProcessResultReader<T> reader) {
    // Add at the beginning of the pre-processors list
    // This ensures that the argument transformers are applied for every argument
    preProcessors.add(0, (problemType, solutionId) -> {
      var outputFile = Paths.get(problemDirectory, outputFileName);
      var normalizedOutputFilePath = outputFile.toString().replace("\\", "/");

      // Add support for replacing the output file path in the arguments
      argumentTransformers.add(
          argument -> argument.replace(OUTPUT_FILE_PATH, normalizedOutputFilePath));

      return Optional.empty();
    });

    return getExecutor((processOutput, processError) -> {
      Path outputFilePath = Path.of(problemDirectory, outputFileName);
      ProcessResult<T> result = reader.read(outputFilePath, Path.of(problemDirectory));

      if (!result.success()) {
        return new ProcessResult<>(
            false,
            result.output(),
            result.errorOutput().isPresent()
                ? Optional.of(result.errorOutput().get()
                + "%nCommand Output: %s".formatted(processOutput))
                : Optional.empty()
        );
      }

      // Return the output
      return result;
    });
  }

  /**
   * Adds an environment variable to the process.
   *
   * @param key The key of the environment variable.
   * @param value The value of the environment variable.
   * @return ProcessRunner instance for chaining.
   */
  public ProcessRunner withEnvironmentVariable(String key, String value) {
    preProcessors.add((problemType, solutionId) -> {
      processBuilder.environment().put(key, value);
      return Optional.empty();
    });

    return this;
  }

  /**
   * Adds arguments to the process.
   *
   * @param arguments The arguments to add to the process.
   * @return ProcessRunner instance for chaining.
   */
  public ProcessRunner withArguments(String... arguments) {
    preProcessors.add((problemType, solutionId) -> {
      for (String argument : arguments) {
        for (var transformer : argumentTransformers) {
          argument = transformer.apply(argument);
        }

        addCommand(argument);
      }

      return Optional.empty();
    });

    return this;
  }

  private void addCommand(String command) {
    List<String> existingCommands = processBuilder.command();
    existingCommands.add(command);
    processBuilder.command(existingCommands);
  }

  protected <T> ProcessRunnerExecutor<T> getExecutor(
      BiFunction<String, String, ProcessResult<T>> outputProcessor) {
    return (problemType, solutionId) -> {
      // Retrieve the problem directory
      try {
        problemDirectory = resourceProvider
            .getProblemDirectory(problemType, solutionId)
            .getAbsolutePath();
      } catch (IOException e) {
        return new ProcessResult<>(
            false,
            Optional.empty(),
            Optional.of("Error: The problem directory couldn't be retrieved:%n%s".formatted(
                e.getMessage()))
        );
      }

      // Run pre-processors
      for (var preProcessor : preProcessors) {
        Optional<Exception> exception = preProcessor.apply(problemType, solutionId);
        if (exception.isPresent()) {
          return new ProcessResult<>(
              false,
              Optional.empty(),
              Optional.of("Error: %s problem couldn't be prepared:%n%s".formatted(
                  problemType.getId(), exception.get().getMessage()))
          );
        }
      }

      // Run the process
      String processOutput;
      String processError;
      int processExitCode;
      try {
        Process process = processBuilder.start();

        processOutput = resourceProvider.readStream(process.inputReader());
        processError = resourceProvider.readStream(process.errorReader());

        processExitCode = process.waitFor();
      } catch (IOException e) {
        return new ProcessResult<>(
            false,
            Optional.empty(),
            Optional.of(
                "Solving %s problem resulted in IO Exception:%n%s".formatted(problemType.getId(),
                    e.getMessage())
            )
        );
      } catch (InterruptedException e) {
        // interrupt current thread:
        Thread.currentThread().interrupt();
        return new ProcessResult<>(
            false, Optional.empty(),
            Optional.of("Thread InterruptedException while running process\n"
                + e.getMessage())
        );
      }

      // Return prematurely if the process failed
      if (processExitCode != 0) {
        return new ProcessResult<>(
            false,
            Optional.empty(),
            Optional.of(
                "%s problem couldn't be solved:%n%s".formatted(problemType.getId(),
                    processOutput + processError)));
      }

      // Run pre-processors
      for (var postProcessor : postProcessors) {
        Optional<Exception> exception = postProcessor.apply(problemType, solutionId);
        if (exception.isPresent()) {
          return new ProcessResult<>(
              false,
              Optional.empty(),
              Optional.of("Error: %s problem couldn't be post-processed:%n%s".formatted(
                  problemType.getId(), exception.get().getMessage()))
          );
        }
      }

      return outputProcessor.apply(processOutput, processError);
    };
  }
}
