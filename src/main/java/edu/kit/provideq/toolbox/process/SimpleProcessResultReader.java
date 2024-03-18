package edu.kit.provideq.toolbox.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class SimpleProcessResultReader implements ProcessResultReader<String> {
    public ProcessResult<String> read(Path solutionPath, Path problemPath, Path problemDirectory) {
        // Read the solution file
        String solutionText;
        try {
            solutionText = Files.readString(solutionPath);
        } catch (IOException e) {
            return new ProcessResult<String>(
                false,
                Optional.empty(),
                Optional.of("Error: The problem data couldn't be read from %s:%n%s%nCommand Output: %s".formatted(
                    solutionPath, e.getMessage()))
            );
        }

        // Return the solution
        return new ProcessResult<String>(
            true,
            Optional.of(solutionText),
                Optional.empty()
        );
    }
}
