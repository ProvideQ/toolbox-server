package edu.kit.provideq.toolbox.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Basic reader for results of ProcessRunners.
 * Reads the content of the file associated with the solutionPath.
 * No specialized processing is done.
 */
public class SimpleProcessResultReader implements ProcessResultReader<String> {
  public ProcessResult<String> read(Path solutionPath, Path problemPath, Path problemDirectory) {
    // Read the solution file
    String solutionText;
    try {
      solutionText = Files.readString(solutionPath);
    } catch (IOException e) {
      return new ProcessResult<>(
          false,
          Optional.empty(),
          Optional.of("Error: The problem data couldn't be read from %s:%n%s%n".formatted(
              solutionPath, e.getMessage()))
      );
    }

    // Return the solution
    return new ProcessResult<>(
        true,
        Optional.of(solutionText),
        Optional.empty()
    );
  }
}
