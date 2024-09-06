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

  public ProcessRunner withInputFile(String inputData) {
    return withInputFile(inputData, INPUT_FILE_NAME);
  }

  public ProcessRunner withInputFile(String inputData, String problemFileName) {
    // Add at the beginning of the pre-processors list
    // This ensures that the argument transformers are applied for every argument
    preProcessors.add(0, (problemType, solutionId) -> {
      var problemFilePath = Paths.get(problemDirectory, problemFileName);
      var normalizedProblemFilePath = problemFilePath.toString().replace("\\", "/");

      // Write the input data to an input file
      try {
        Files.writeString(problemFilePath, inputData);
      } catch (IOException e) {
        return Optional.of(new IOException(
            "Error: The input data couldn't be written to %s:%n%s".formatted(
                normalizedProblemFilePath, e.getMessage()),
            e));
      }

      // Add support for replacing the input file path in the arguments
      argumentTransformers.add(
          argument -> argument.replace(INPUT_FILE_PATH, normalizedProblemFilePath));

      return Optional.empty();
    });

    return this;
  }

  public ProcessRunnerExecutor<String> withOutputString() {
    return getExecutor((processOutput, processError) -> new ProcessResult<>(
            true,
            Optional.of(processOutput),
            Optional.empty()
        )
    );
  }

  public ProcessRunnerExecutor<String> withOutputFile() {
    return withOutputFile(OUTPUT_FILE_NAME);
  }

  public ProcessRunnerExecutor<String> withOutputFile(String outputFileName) {
    return withOutputFile(outputFileName, new SimpleProcessResultReader());
  }

  public <T> ProcessRunnerExecutor<T> withOutputFile(ProcessResultReader<T> reader) {
    return withOutputFile(OUTPUT_FILE_NAME, reader);
  }

  public <T> ProcessRunnerExecutor<T> withOutputFile(String outputFileName,
                                                     ProcessResultReader<T> reader) {
    // Add at the beginning of the pre-processors list
    // This ensures that the argument transformers are applied for every argument
    preProcessors.add(0, (problemType, solutionId) -> {
      // Retrieve the problem directory
      String problemDirectoryPath;
      try {
        problemDirectoryPath = resourceProvider
            .getProblemDirectory(problemType, solutionId)
            .getAbsolutePath();
      } catch (IOException e) {
        return Optional.of(new IOException(
            "Error: The problem directory couldn't be retrieved:%n%s".formatted(e.getMessage()),
            e));
      }

      var outputFile = Paths.get(problemDirectoryPath, outputFileName);
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

  public ProcessRunner withEnvironmentVariable(String key, String value) {
    preProcessors.add((problemType, solutionId) -> {
      processBuilder.environment().put(key, value);
      return Optional.empty();
    });

    return this;
  }

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
