package edu.kit.provideq.toolbox.process;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

/**
 * A reader that reads the output of a ProcessRunner that created multiple files.
 * The content of the files is combined to a HashMap (File path : File content (String)).
 */
public class MultiFileProcessResultReader implements ProcessResultReader<HashMap<Path, String>> {

  private final String globPattern;

  public MultiFileProcessResultReader(String globPattern) {
    this.globPattern = globPattern;
  }

  public ProcessResult<HashMap<Path, String>> read(Path solutionPath, Path problemPath,
                                                   Path problemDirectory) {

    HashMap<Path, String> solutions = new HashMap<>();

    // Split globPattern at the last slash
    String directoryPath = globPattern.substring(0, globPattern.lastIndexOf('/'));
    String filePattern = globPattern.substring(globPattern.lastIndexOf('/') + 1);

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(
        Path.of(problemDirectory.toString(), directoryPath), filePattern)) {
      for (Path file : stream) {
        solutions.put(file, Files.readString(file));
      }
    } catch (IOException e) {
      return new ProcessResult<>(
          false,
          Optional.empty(),
          Optional.of("Error: The problem data couldn't be read from %s:%n%s%nCommand".formatted(
              solutionPath, e.getMessage()))
      );
    }

    // Return the solution
    return new ProcessResult<>(
        true,
        Optional.of(solutions),
        Optional.empty()
    );
  }
}
